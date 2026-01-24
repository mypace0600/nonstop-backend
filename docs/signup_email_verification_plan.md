# 회원가입 이메일 인증 기능 구현 계획

## 1. 개요
회원가입 시 입력한 이메일의 실제 소유 여부를 확인하기 위한 인증 절차를 구현합니다.
사용자가 모든 정보를 입력한 후 **인증 대기 상태**로 저장하고, 이메일 인증 완료 시 가입이 완료되는 방식입니다.

### 1.1 기존 시스템과의 차이점
| 구분 | 기존 대학 인증 (웹메일) | 회원가입 이메일 인증 |
|------|----------------------|-------------------|
| 목적 | 대학생 신분 인증 | 이메일 소유권 확인 |
| 시점 | 가입 후 언제든지 | 가입 시 필수 |
| 대상 이메일 | 학교 웹메일만 | 모든 이메일 |
| 인증 후 효과 | `is_verified=true` | `email_verified=true` |
| Redis 키 | `verification:email:{userId}` | `signup:verification:{email}` |

## 2. 데이터베이스 설계

### 2.1 users 테이블 변경
```sql
-- 컬럼 추가
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN email_verified_at TIMESTAMP;

-- 기존 사용자 마이그레이션 (기존 회원은 인증된 것으로 처리)
UPDATE users SET email_verified = TRUE, email_verified_at = created_at WHERE email_verified = FALSE;
```

### 2.2 User 엔티티 변경
```java
// User.java에 추가
private Boolean emailVerified;      // 이메일 인증 여부
private LocalDateTime emailVerifiedAt;  // 이메일 인증 일시
```

## 3. Redis 설계

### 3.1 인증 코드 저장
- **Key**: `signup:verification:{email}`
- **Value**: JSON 객체
  ```json
  {
    "code": "123456",
    "userId": 123,
    "createdAt": "2026-01-21T10:30:00"
  }
  ```
- **TTL**: 300초 (5분)

### 3.2 Rate Limit (재발송 제한)
- **Key**: `signup:resend:limit:{email}`
- **Value**: `1`
- **TTL**: 60초 (1분)

## 4. API 명세

### 4.1 회원가입 요청 (변경)
**Endpoint:** `POST /api/v1/auth/signup`

#### Request Body
```json
{
  "email": "user@example.com",
  "password": "securePassword123!",
  "nickname": "논스톱",
  "agreements": {
    "termsOfService": true,
    "privacyPolicy": true,
    "marketingConsent": false
  }
}
```

#### Process
1. 입력값 유효성 검증
2. 이메일 중복 체크
   - 인증 완료된 사용자 존재 → `409 Conflict` 반환
   - 인증 대기 사용자 존재 → 기존 데이터 삭제 후 진행
3. 닉네임 중복 체크
4. 사용자 정보 저장 (`email_verified=false`)
5. 6자리 인증 코드 생성 및 Redis 저장
6. 인증 메일 발송

#### Response
- **201 Created**
```json
{
  "success": true,
  "data": {
    "message": "인증 메일이 발송되었습니다. 5분 내에 인증을 완료해주세요."
  }
}
```

#### Error Cases
| 상황 | HTTP Status | 에러 코드 |
|------|-------------|----------|
| 이메일 중복 (인증 완료) | 409 Conflict | DUPLICATE_EMAIL |
| 닉네임 중복 | 409 Conflict | DUPLICATE_NICKNAME |
| 유효성 검증 실패 | 400 Bad Request | VALIDATION_ERROR |
| 메일 발송 실패 | 500 Internal Server Error | EMAIL_SEND_FAILED |

---

### 4.2 이메일 인증 코드 확인
**Endpoint:** `POST /api/v1/auth/signup/verify`

#### Request Body
```json
{
  "email": "user@example.com",
  "code": "123456"
}
```

#### Process
1. Redis에서 `signup:verification:{email}` 조회
2. 코드 존재 여부 및 일치 여부 확인
3. 인증 성공 시:
   - `users.email_verified = true` 업데이트
   - `users.email_verified_at = now()` 업데이트
   - Redis 키 삭제
   - JWT 토큰 발급 (자동 로그인)

#### Response
- **200 OK**
```json
{
  "success": true,
  "data": {
    "userId": 123,
    "accessToken": "eyJhbG...",
    "refreshToken": "eyJhbG..."
  }
}
```

#### Error Cases
| 상황 | HTTP Status | 에러 코드 |
|------|-------------|----------|
| 인증 코드 만료/없음 | 400 Bad Request | VERIFICATION_CODE_EXPIRED |
| 인증 코드 불일치 | 400 Bad Request | VERIFICATION_CODE_MISMATCH |
| 이미 인증된 사용자 | 400 Bad Request | ALREADY_VERIFIED |
| 사용자 없음 | 404 Not Found | USER_NOT_FOUND |

---

### 4.3 인증 코드 재발송
**Endpoint:** `POST /api/v1/auth/signup/resend`

#### Request Body
```json
{
  "email": "user@example.com"
}
```

#### Process
1. 해당 이메일의 인증 대기 사용자 조회
2. Rate Limit 체크 (`signup:resend:limit:{email}`)
3. 기존 인증 코드 삭제
4. 새 인증 코드 생성 및 Redis 저장
5. 인증 메일 재발송
6. Rate Limit 키 설정 (TTL 60초)

#### Response
- **200 OK**
```json
{
  "success": true,
  "data": {
    "message": "인증 메일이 재발송되었습니다."
  }
}
```

#### Error Cases
| 상황 | HTTP Status | 에러 코드 |
|------|-------------|----------|
| 인증 대기 사용자 없음 | 404 Not Found | USER_NOT_FOUND |
| 이미 인증된 사용자 | 400 Bad Request | ALREADY_VERIFIED |
| 재발송 제한 (1분) | 429 Too Many Requests | RESEND_RATE_LIMITED |

## 5. 로그인 응답 변경

### 5.1 TokenResponseDto 변경
미인증 사용자도 로그인 가능하며, 응답에 `emailVerified` 필드를 추가합니다.

```java
@Data
@Builder
public class TokenResponseDto {
    private Long userId;
    private String accessToken;
    private String refreshToken;
    private Boolean emailVerified;  // 추가
}
```

### 5.2 로그인 응답 예시
#### 인증 완료 사용자
```json
{
  "success": true,
  "data": {
    "userId": 123,
    "accessToken": "eyJhbG...",
    "refreshToken": "eyJhbG...",
    "emailVerified": true
  }
}
```

#### 미인증 사용자
```json
{
  "success": true,
  "data": {
    "userId": 123,
    "accessToken": "eyJhbG...",
    "refreshToken": "eyJhbG...",
    "emailVerified": false
  }
}
```

### 5.3 클라이언트 처리
- `emailVerified: false`인 경우 클라이언트에서 이메일 인증 화면으로 유도
- 인증 완료 전에도 앱 기능 사용 가능 (정책에 따라 일부 기능 제한 가능)

## 6. 스케줄러: 미인증 사용자 정리

### 6.1 목적
24시간 내에 이메일 인증을 완료하지 않은 사용자 데이터를 자동 삭제합니다.

### 6.2 구현
```java
@Scheduled(cron = "0 0 3 * * *")  // 매일 새벽 3시 실행
public void cleanupUnverifiedUsers() {
    LocalDateTime threshold = LocalDateTime.now().minusHours(24);
    int deletedCount = authMapper.deleteUnverifiedUsersBefore(threshold);
    log.info("Cleaned up {} unverified users", deletedCount);
}
```

### 6.3 SQL 쿼리
```sql
DELETE FROM users
WHERE email_verified = FALSE
  AND created_at < #{threshold}
  AND auth_provider = 'EMAIL';
```

## 7. 이메일 템플릿

### 7.1 인증 메일 내용
**제목:** [Nonstop] 회원가입 이메일 인증

**본문:**
```
안녕하세요, Nonstop입니다.

회원가입을 위한 이메일 인증 코드입니다.

인증 코드: {code}

이 코드는 5분간 유효합니다.
본인이 요청하지 않은 경우, 이 메일을 무시해주세요.

감사합니다.
Nonstop 팀
```

## 8. 구현 순서

### Phase 1: 기반 작업
1. [ ] `users` 테이블에 `email_verified`, `email_verified_at` 컬럼 추가
2. [ ] `User` 엔티티 및 Mapper 업데이트
3. [ ] `AuthMapper`에 새 쿼리 추가
   - `findByEmailAndNotVerified()`
   - `deleteByEmailAndNotVerified()`
   - `updateEmailVerified()`
   - `deleteUnverifiedUsersBefore()`

### Phase 2: 서비스 로직
4. [ ] `SignupVerificationService` 생성 (또는 `AuthService` 확장)
   - `initiateSignup()`: 회원가입 + 인증 코드 발송
   - `verifySignupCode()`: 코드 검증 + 토큰 발급
   - `resendVerificationCode()`: 재발송
5. [ ] `EmailService`에 회원가입 인증 메일 발송 메서드 추가
6. [ ] Redis 연동 (`signup:verification:*` 키 관리)

### Phase 3: 컨트롤러 및 예외 처리
7. [ ] `AuthController`에 새 엔드포인트 추가
   - `POST /api/v1/auth/signup` (기존 로직 변경)
   - `POST /api/v1/auth/signup/verify`
   - `POST /api/v1/auth/signup/resend`
8. [ ] 커스텀 예외 클래스 생성
   - `VerificationCodeExpiredException`
   - `VerificationCodeMismatchException`
   - `ResendRateLimitedException`
9. [ ] `TokenResponseDto`에 `emailVerified` 필드 추가 및 로그인 응답 수정

### Phase 4: 스케줄러 및 마무리
10. [x] 미인증 사용자 정리 스케줄러 구현
11. [x] 기존 데이터 마이그레이션 스크립트 작성
12. [x] 테스트 작성 및 검증

## 9. 의존성

### 9.1 필요 라이브러리
- Spring Boot Starter Mail (기존 사용 중)
- Spring Data Redis (기존 사용 중)

### 9.2 환경 변수
```yaml
# application.yml
spring:
  mail:
    host: ${MAIL_HOST}
    port: ${MAIL_PORT}
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
```

## 10. 테스트 시나리오

### 10.1 정상 플로우
1. 회원가입 요청 → 인증 메일 수신 → 코드 입력 → 가입 완료 → 로그인 성공

### 10.2 예외 케이스
1. 잘못된 인증 코드 입력 → 에러 메시지
2. 인증 코드 만료 후 입력 → 에러 + 재발송 안내
3. 1분 내 재발송 시도 → Rate Limit 에러
4. 미인증 상태로 로그인 시도 → 로그인 성공 + `emailVerified: false` 응답
5. 인증 대기 상태에서 동일 이메일로 재가입 → 기존 데이터 삭제 후 진행
6. 24시간 후 미인증 사용자 → 스케줄러에 의해 삭제

## 11. 보안 고려사항

1. **인증 코드 보안**: 6자리 숫자 난수 (SecureRandom 사용)
2. **Rate Limiting**: 재발송 1분 제한, 향후 일일 총 발송 횟수 제한 고려
3. **Brute Force 방지**: 코드 입력 시도 횟수 제한 고려 (5회 실패 시 새 코드 발급 필요)
4. **이메일 열거 공격 방지**: 존재하지 않는 이메일에도 동일한 응답 반환 고려

---

**작성일**: 2026-01-21
**버전**: v1.0
**관련 PRD**: v2.5.12 - 3.1.7 회원가입 이메일 인증

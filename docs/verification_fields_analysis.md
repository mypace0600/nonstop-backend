# is_verified vs email_verified 필드 분석

**작성일**: 2026-01-24
**버전**: v1.0
**관련 파일**: `migrations.sql`, `User.java`, `signup_email_verification_plan.md`, `university_email_verification_plan.md`

---

## 1. 현재 구조 분석

### 1.1 users 테이블의 인증 관련 필드

```sql
-- users 테이블 (migrations.sql)
is_verified BOOLEAN NOT NULL DEFAULT FALSE,        -- 대학생 인증 여부
verification_method verification_method,           -- EMAIL_DOMAIN, MANUAL_REVIEW, STUDENT_ID_PHOTO
email_verified BOOLEAN NOT NULL DEFAULT FALSE,     -- 회원가입 이메일 인증 여부
email_verified_at TIMESTAMP,                       -- 이메일 인증 완료 시각
```

### 1.2 두 필드의 역할 비교

| 구분 | `is_verified` | `email_verified` |
|------|---------------|------------------|
| **목적** | 대학생 신분 인증 | 이메일 소유권 확인 (본인인증) |
| **인증 시점** | 가입 후 언제든지 | 가입 시 필수 |
| **대상 이메일** | 학교 웹메일만 (예: user@korea.ac.kr) | 모든 이메일 (개인 이메일 포함) |
| **관련 필드** | `verification_method`, `university_id` | `email_verified_at` |
| **Redis 키** | `verification:email:{userId}` | `signup:verification:{email}` |
| **API 엔드포인트** | `/api/v1/verification/email/*` | `/api/v1/auth/email/*` |

### 1.3 User 엔티티 (User.java)

```java
private Boolean isVerified;              // 대학생 인증 여부
private Boolean emailVerified;           // 회원가입 이메일 인증 여부
private LocalDateTime emailVerifiedAt;   // 이메일 인증 완료 시각
private VerificationMethod verificationMethod;  // 대학 인증 방식
```

---

## 2. 인증 프로세스 상세

### 2.1 회원가입 이메일 인증 (`email_verified`)

**목적**: 사용자가 입력한 이메일의 실제 소유자인지 확인

**플로우**:
1. `POST /api/v1/auth/signup` - 회원정보 저장 (`email_verified=false`)
2. `POST /api/v1/auth/email/send-verification` - 인증 코드 발송
3. `POST /api/v1/auth/email/verify` - 코드 검증 → `email_verified=true`

**특징**:
- 미인증 상태로 24시간 경과 시 자동 삭제 (스케줄러)
- 미인증 상태에서도 로그인 가능 (응답에 `emailVerified: false` 포함)

### 2.2 대학생 인증 (`is_verified`)

**목적**: 해당 대학의 재학생/졸업생임을 증명

**인증 방식**:
| 방식 | 설명 | `verification_method` |
|------|------|----------------------|
| 학교 웹메일 인증 | 학교 이메일로 인증 코드 발송 → 검증 | `EMAIL_DOMAIN` |
| 학생증 사진 인증 | 사진 업로드 → 관리자 수동 검토 | `STUDENT_ID_PHOTO` |
| 수동 승인 | 운영자가 직접 승인 | `MANUAL_REVIEW` |

**플로우 (웹메일 인증)**:
1. `POST /api/v1/verification/email/request` - 학교 이메일로 인증 코드 발송
2. `POST /api/v1/verification/email/confirm` - 코드 검증 → `is_verified=true`, `university_id` 업데이트

**효과**:
- 대학교별 커뮤니티 접근 권한 부여
- Access Token Payload에 `isVerified: true` 포함

---

## 3. 문제점

### 3.1 명명의 모호성

- **`is_verified`**: 이름만 보면 "무엇이" 인증되었는지 불분명
  - "사용자 인증"인지, "이메일 인증"인지, "대학 인증"인지 알 수 없음
- **`email_verified`**: 대학 인증도 이메일로 진행하기 때문에 혼동 가능
  - 둘 다 "이메일 인증"처럼 보임

### 3.2 API 응답에서의 혼란

로그인 응답 (PRD 3.1.2):
```json
{
  "userId": 123,
  "accessToken": "eyJhbG...",
  "refreshToken": "eyJhbG...",
  "isVerified": true,        // 대학생 인증
  "emailVerified": true      // 이메일 소유권 확인
}
```

- 클라이언트 개발자 입장에서 두 필드의 차이를 직관적으로 이해하기 어려움
- 문서를 읽지 않으면 용도 파악 불가

### 3.3 시나리오 중복 가능성

**케이스**: 학교 이메일(예: user@korea.ac.kr)로 회원가입하는 경우

- 현재: 가입 시 `email_verified` 인증 + 별도로 대학 인증 시 `is_verified` 인증 (2번 인증)
- 개선 가능: 학교 이메일로 가입하면 한번에 둘 다 처리

### 3.4 일관성 부족

- `is_verified`는 `is_` prefix 사용
- `email_verified`는 prefix 없이 `_verified` suffix만 사용
- 네이밍 컨벤션 불일치

---

## 4. 개선 방안

### Option A: 명명 개선 (권장)

**변경 내용**:
```
is_verified → is_university_verified (또는 university_verified)
```

**영향 범위**:
- DB 컬럼명 변경 (마이그레이션 필요)
- User 엔티티 필드명 변경
- Mapper XML 쿼리 수정
- API 응답 DTO 필드명 변경
- Access Token Payload 필드명 변경

**장점**:
- 의미가 명확해짐
- 코드 가독성 향상

**단점**:
- API Breaking Change → 클라이언트 수정 필요
- 마이그레이션 작업 필요

### Option B: DTO 레벨에서만 명확화

**변경 내용**:
```java
// TokenResponseDto
private Boolean universityVerified;    // DB: is_verified
private Boolean signupEmailVerified;   // DB: email_verified
```

**장점**:
- DB 변경 없음
- 마이그레이션 불필요

**단점**:
- DB와 API 필드명 불일치로 관리 복잡
- 여전히 내부 코드는 모호한 명명 유지

### Option C: 기능 통합 (학교 이메일 가입 시 동시 인증)

**변경 내용**:
- 가입 이메일이 `university_email_domains` 테이블의 도메인과 일치하면
- 회원가입 인증 완료 시 `email_verified` + `is_verified` 동시 처리

**장점**:
- UX 개선 (중복 인증 절차 제거)
- 학교 이메일 사용자의 가입 전환율 향상

**단점**:
- 구현 복잡도 증가
- 기존 로직 수정 필요

### Option D: 문서화만 강화 (최소 변경)

**변경 내용**:
- 코드 주석 강화
- API 문서에 명확한 설명 추가
- 필드명은 유지

**장점**:
- 코드 변경 없음
- 리스크 최소

**단점**:
- 근본적인 해결 아님
- 신규 개발자 온보딩 시 여전히 혼란

---

## 5. 권장 방안

**1단계 (단기)**: Option A 적용
- `is_verified` → `is_university_verified`로 명명 변경
- API 버전 업그레이드와 함께 진행

**2단계 (중기)**: Option C 검토
- 학교 이메일 가입 시 동시 인증 기능 추가
- 사용자 피드백 수집 후 결정

---

## 6. 참고 문서

- `docs/signup_email_verification_plan.md` - 회원가입 이메일 인증 계획
- `docs/uni/university_email_verification_plan.md` - 대학교 웹메일 인증 계획
- `docs/prd_draft.md` - PRD 3.1.7 (정책 동의), 3.1.8 (이메일 인증), 3.3 (대학 인증)
- `docs/db/migrations.sql` - V7 마이그레이션 (email_verified 추가)

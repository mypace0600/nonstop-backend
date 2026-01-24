# Auth Module Implementation Plan (v2.5.17)

## 1. 개요
PRD v2.5.17에 정의된 **회원가입 이메일 인증**, **만 14세 미만 가입 제한**, **로그인/로그아웃 이력 관리** 기능을 구현하기 위한 상세 계획입니다.

## 2. 구현 목표
1.  **회원가입 프로세스 변경**: 즉시 가입 완료 → **이메일 인증 대기** 상태로 전환.
2.  **만 14세 미만 방지**: 가입 및 생년월일 등록 시 나이 계산 로직 추가.
3.  **이메일 소유권 검증**: Redis를 활용한 인증 코드 발송 및 검증.
4.  **보안 감사**: 로그인 및 로그아웃 활동 이력 기록.

## 3. 상세 구현 단계

### Phase 1: 도메인 모델 및 인프라 정비
- [ ] **User Entity 업데이트**
    - `emailVerified` (Boolean), `emailVerifiedAt` (LocalDateTime), `birthDate` (LocalDate) 필드 추가.
    - MyBatis `UserMapper.xml` ResultMap 및 쿼리 업데이트.
- [ ] **LoginHistory 도메인 생성**
    - `LoginHistory` 엔티티 클래스 생성.
    - `LoginHistoryMapper` (Interface & XML) 생성 (`save` 메서드).

### Phase 2: 회원가입 로직 고도화 (AuthService)
- [ ] **DTO 업데이트**
    - `SignUpRequestDto`: `birthDate` 필드 추가 (필수).
    - `TokenResponseDto`: `emailVerified`, `hasBirthDate` 필드 추가.
- [ ] **생년월일 검증 로직 (`AgeValidator`)**
    - 입력된 생년월일 기준 만 14세 미만 여부 체크.
    - `400 Bad Request` (Code: `UNDER_AGE_LIMIT`) 예외 처리.
- [ ] **회원가입 (`signUp`) 메서드 변경**
    - 나이 체크 수행.
    - DB 저장 시 `email_verified = false` 설정.
    - **인증 코드 생성 및 발송**:
        - 6자리 난수 생성.
        - Redis 저장 (`signup:verification:{email}`, TTL 5분).
        - 이메일 발송 (비동기 처리 권장).
    - 응답: 토큰 대신 "인증 메일 발송됨" 메시지 반환.

### Phase 3: 이메일 인증 및 재발송 API 구현
- [ ] **인증 코드 검증 (`verifyEmail`)**
    - Redis에서 코드 조회 및 비교.
    - 성공 시:
        - DB 업데이트 (`email_verified = true`, `email_verified_at = now`).
        - Redis 키 삭제.
        - **JWT 토큰 발급 및 반환**.
- [ ] **인증 코드 재발송 (`resendVerificationCode`)**
    - 미인증 사용자 확인.
    - Rate Limit 체크 (Redis 활용, 1분 제한).
    - 코드 재생성, Redis 갱신, 메일 재발송.

### Phase 4: 로그인 이력 및 기타 User 기능
- [ ] **로그인 이력 기록**
    - `AuthService.login` 및 `googleLogin` 성공 시 `LoginHistory` 저장 (TYPE: LOGIN).
    - `AuthService.logout` 성공 시 `LoginHistory` 저장 (TYPE: LOGOUT).
    - IP 주소 및 User-Agent 파싱 유틸리티 활용.
- [ ] **생년월일 등록 API (`registerBirthDate`)**
    - 기존 가입자 중 생년월일 없는 유저 대상.
    - 나이 체크 후 DB 업데이트.
- [ ] **로그인 응답(`TokenResponseDto`) 데이터 채우기**
    - `emailVerified`, `hasBirthDate` 필드 매핑.

### Phase 5: 스케줄러 및 정리
- [ ] **미인증 사용자 정리 스케줄러**
    - 매일 새벽, 24시간 지난 미인증 계정(`email_verified=false`) 삭제 (Soft Delete 아님, Hard Delete).

## 4. API 명세 변경 요약

| Method | URI | 변경/신규 | 설명 |
|---|---|---|---|
| POST | `/api/v1/auth/signup` | 변경 | `birthDate` 추가, 토큰 미반환(메시지 반환) |
| POST | `/api/v1/auth/signup/verify` | **신규** | 인증 코드 검증 및 토큰 발급 |
| POST | `/api/v1/auth/signup/resend` | **신규** | 인증 코드 재발송 |
| POST | `/api/v1/users/me/birth-date` | **신규** | 생년월일 사후 등록 |

## 5. 데이터베이스 변경 (기 완료 - V7)
- `users` 테이블: `email_verified`, `email_verified_at`, `birth_date` 컬럼 추가됨.
- `login_history` 테이블: 생성됨.

## 6. 작업 순서
1. `User` Entity & Mapper 수정
2. `LoginHistory` 구현
3. `AuthService` - 회원가입 로직 변경 (나이 체크, 미인증 저장)
4. `EmailService` & Redis 연동
5. `verify` / `resend` 구현
6. `AuthController` 엔드포인트 추가
7. 테스트

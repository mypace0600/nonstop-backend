# 대학교 웹메일 인증 기능 구현 계획

## 1. 개요
사용자가 입력한 학교 웹메일(`user@univ.ac.kr`)로 인증 코드를 발송하고, 이를 검증하여 해당 대학의 학생임을 인증하는 기능을 구현합니다.
기존의 도메인 매칭 방식(자동 승인)에서 **실제 이메일 소유 확인(Challenge-Response)** 방식으로 보안을 강화합니다.

## 2. 데이터베이스 및 Redis 설계

### 2.1 Database (PostgreSQL)
기존 `university_email_domains` 테이블을 활용하여 이메일 도메인과 대학 정보를 매핑합니다.

- **관련 테이블:**
  - `users`: 사용자 정보 (`university_id`, `is_verified`, `verification_method` 업데이트)
  - `universities`: 대학 기본 정보
  - `university_email_domains`: 대학별 이메일 도메인 매핑 (예: `korea.ac.kr` -> 고려대학교 ID)

### 2.2 Redis (In-Memory)
인증 코드를 일시적으로 저장하고 유효 시간을 관리하기 위해 사용합니다.

- **Key 구조:** `verification:email:{userId}`
- **Value:** `{ "code": "123456", "email": "user@univ.ac.kr", "universityId": 1 }`
- **TTL (유효 시간):** 5분 (300초)

## 3. API 명세 (API Specifications)

### 3.1 인증 코드 요청
**Endpoint:** `POST /api/v1/verification/email/request`

- **Request Body:**
  ```json
  {
    "email": "myid@korea.ac.kr"
  }
  ```
- **Process:**
  1. 요청자(User) 확인 (로그인 필수).
  2. 이메일 주소에서 도메인 추출 (예: `korea.ac.kr`).
  3. `UniversityMapper`를 통해 해당 도메인의 `universityId` 조회.
     - 매칭되는 대학이 없으면 `404 Not Found` 또는 `400 Bad Request` 반환.
  4. 6자리 난수 인증 코드 생성.
  5. Redis에 `userId`를 키로 인증 정보 저장 (TTL 5분).
  6. JavaMailSender를 사용하여 해당 이메일로 인증 코드 발송.
- **Response:** `200 OK`

### 3.2 인증 코드 확인
**Endpoint:** `POST /api/v1/verification/email/confirm`

- **Request Body:**
  ```json
  {
    "code": "123456"
  }
  ```
- **Process:**
  1. 요청자(User) 확인.
  2. Redis에서 `verification:email:{userId}` 조회.
  3. 저장된 인증 코드와 입력된 코드 비교.
     - 불일치 시 `400 Bad Request` 반환.
     - 만료 시 `400 Bad Request` (Time expired) 반환.
  4. 검증 성공 시:
     - `users` 테이블 업데이트:
       - `university_id`: Redis에 저장된 값
       - `is_verified`: `true`
       - `verification_method`: `EMAIL_DOMAIN`
     - Redis 키 삭제 (일회용).
- **Response:** `200 OK`

## 4. 구현 상세 로직

### 4.1 Backend Components

#### `UniversityMapper`
- `findByDomain(String domain)`: 도메인 문자열로 `University` 엔티티(ID 포함) 조회.

#### `VerificationService`
- `sendEmailVerificationCode(Long userId, String email)`:
  - 도메인 검증 및 코드 생성/저장/발송 로직.
- `confirmEmailVerification(Long userId, String code)`:
  - 코드 검증 및 사용자 정보 업데이트 로직.

#### `EmailService` (신규 유틸리티 서비스)
- `sendSimpleMessage(String to, String subject, String text)`: Spring MailSender 래핑.

### 4.2 예외 처리
- **Invalid Domain:** 지원하지 않는 학교 도메인인 경우.
- **Rate Limiting:** 이메일 발송 횟수 제한 (Bucket4j 활용 가능, 1분당 1회 등).
- **Already Verified:** 이미 인증된 사용자가 재요청하는 경우 (정책에 따라 허용/차단).

## 5. 작업 순서
1. `UniversityMapper` 쿼리 추가 (`findByDomain`).
2. `EmailService` 구현 (SMTP 연동).
3. `VerificationService`에 이메일 인증 로직 구현.
4. `VerificationController`에 API 엔드포인트 추가.
5. 테스트 및 검증.

# Nonstop App – Product Requirements Document
**Golden Master v2.5.12 (2026.01 Backend Status: 85% Completed)**

## 1. Overview
대학생 전용 실명 기반 커뮤니티 모바일 앱  
핵심 가치: 학교 인증 없이도 바로 사용 가능 → 자연스럽게 학교 입력 및 학생 인증 유도

## 2. Target Users & Core Journey
- 신입생 → 학교·전공 선택 → 학생 인증 → 커뮤니티/시간표 이용
- 재학생 → 친구·채팅·게시판·시간표 공유 중심

## 3. Functional Requirements

### 3.1 Authentication (핵심)

#### 3.1.1 인증 방식 및 토큰 정책
- 모든 로그인 방식 동일 JWT 기반
- Access Token: 30분 (payload에 universityId null 허용)
- Refresh Token: 30일, DB 저장 (`refresh_tokens`), 개별 무효화 가능
- 모든 보호 API: `Authorization: Bearer <accessToken>` 필수

| 토큰 종류 | 유효 기간 | 저장소 (Client) | 저장소 (Server) | 용도 |
|---|---|---|---|---|
| Access Token | 30분 | Secure Storage | 없음 (Stateless) | API 요청 인증 |
| Refresh Token | 30일 | Secure Storage | DB (refresh_tokens) | Access Token 재발급 |

**Refresh Token Rotation (RTR):**
- Access Token 재발급 시 Refresh Token도 함께 재발급(교체)
- 한 번 사용된 Refresh Token은 즉시 폐기 (soft-delete via `revoked_at`)
- 탈취된 Refresh Token의 재사용 방지
- 토큰 사용 기록 추적 가능 (보안 감사용, v2.5.11)

#### 3.1.2 자동 로그인 (Auto Login)
자동 로그인은 별도 API가 아닌, 클라이언트가 저장된 토큰으로 세션을 복구하는 과정입니다.

**프로세스:**
1. 앱 실행 시 Secure Storage에서 토큰 로드
2. Access Token 유효 → 메인 화면 진입 (자동 로그인 성공)
3. Access Token 만료 (401 응답) → Refresh 요청
   - Refresh 성공 → 새 토큰 저장 후 원래 요청 재시도
   - Refresh 실패 → 로컬 토큰 삭제 후 로그인 화면 이동

**클라이언트 구현 요구사항:**
- 토큰 저장: Android Keystore / iOS Keychain 사용 필수
- API 인터셉터: 모든 요청에 `Authorization` 헤더 자동 추가
- 401 처리: 요청 큐잉 → Refresh 시도 → 성공 시 재시도 / 실패 시 로그아웃

**Refresh API 명세:**
- `POST /api/v1/auth/refresh`
- Request: `{ "refreshToken": "..." }`
- Response (성공): `{ "userId": 123, "accessToken": "...", "refreshToken": "..." }` (userId + 새 토큰 쌍)
- Response (실패): `401 Unauthorized` 또는 `403 Forbidden`

**로그인 응답 공통 형식 (v2.5.10):**
모든 로그인/토큰 재발급 API 응답에는 `userId`가 포함됩니다:
```json
{
  "userId": 123,
  "accessToken": "eyJhbG...",
  "refreshToken": "eyJhbG..."
}
```

#### 3.1.3 지원 로그인 방식
- 이메일 + 비밀번호 (bcrypt)
- Google OAuth 2.0 (모바일 SDK → credential → 백엔드 검증)
  - **프로필 동기화 (v2.5.11)**: 기존 사용자 재로그인 시 Google 프로필 이미지 변경 감지 및 자동 업데이트

#### 3.1.4 Access Token Payload (표준)
```
{
  "sub": "12345",            // userId (String)
  "auth": "ROLE_USER",       // Authorities (Comma separated)
  "email": "hello@korea.ac.kr",
  "universityId": 52,        // null 허용
  "isVerified": true,        // 대학생 인증 여부
  "iat": 1735999999,
  "exp": 1736001799
}
```

#### 3.1.5 universityId = null 허용 정책 (Graceful Degradation)
가능 기능: 프로필, 친구, 1:1 채팅, 알림, 내 시간표 관리, **공통 커뮤니티 이용**
제한 기능 (universityRequired = true 반환):
- **학교별** 커뮤니티/게시판 이용
- 공개 시간표 조회/공개
- 일부 익명 게시판 (운영 정책에 따라)

#### 3.1.6 회원가입 시 약관 동의 (Terms & Consent)
회원가입 시 법적 요구사항을 충족하기 위한 약관 동의 절차입니다.

##### 동의 항목
| 항목 | 필수 여부 | 설명 |
|------|----------|------|
| **서비스 이용약관** | 필수 | 서비스 이용에 관한 기본 약관 |
| **개인정보 수집 및 이용** | 필수 | 개인정보 처리방침 동의 |
| **마케팅 정보 수신** | 선택 | 푸시 알림, 이메일 마케팅 수신 동의 |

##### 회원가입 API 변경
**이메일 회원가입 (`POST /api/v1/auth/signup`)**
```json
{
  "email": "user@example.com",
  "password": "securePassword123!",
  "nickname": "논스톱",
  "agreements": {
    "termsOfService": true,       // 필수
    "privacyPolicy": true,        // 필수
    "marketingConsent": false     // 선택
  }
}
```
- 필수 항목 미동의 시: `400 Bad Request` ("필수 약관에 동의해야 합니다.")

**Google OAuth 회원가입 (`POST /api/v1/auth/google`)**
- 최초 가입 시 약관 동의 필요
- 기존 회원 로그인 시 동의 불필요 (기존 동의 내역 유지)
- Response에 `isNewUser: true` 포함 시, 클라이언트에서 약관 동의 화면 표시 후 동의 정보 전송

##### 데이터 모델
```sql
CREATE TABLE user_agreements (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id),
  agreement_type VARCHAR(30) NOT NULL,  -- TERMS_OF_SERVICE, PRIVACY_POLICY, MARKETING
  agreed BOOLEAN NOT NULL DEFAULT FALSE,
  agreed_at TIMESTAMP,
  ip_address VARCHAR(45),               -- 동의 시점 IP (법적 증빙용)
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now(),
  UNIQUE(user_id, agreement_type)
);
```

##### 동의 내역 관리
- **동의 내역 조회 (`GET /api/v1/users/me/agreements`)**
  - 현재 동의 상태 및 동의 일시 반환
- **동의 변경 (`PATCH /api/v1/users/me/agreements`)**
  - 선택 항목(마케팅 등)만 변경 가능
  - 필수 항목 철회 시: 회원 탈퇴 안내
- **약관 문서 조회 (`GET /api/v1/terms/{type}`)**
  - `type`: `terms-of-service`, `privacy-policy`, `marketing`
  - 최신 약관 내용(HTML/Markdown) 및 버전 반환

##### 약관 버전 관리
- 약관 내용 변경 시 버전 업데이트
- 중요 변경 시 기존 사용자에게 재동의 요청 (앱 내 팝업)
- 재동의 거부 시 서비스 이용 제한 가능

#### 3.1.7 회원가입 이메일 인증 (Signup Email Verification)
회원가입 시 입력한 이메일의 실제 소유 여부를 확인하기 위한 인증 절차입니다.

##### 인증 플로우
1. **회원가입 요청 (`POST /api/v1/auth/signup`)**
   - 사용자가 이메일, 비밀번호, 닉네임, 약관 동의 정보를 입력
   - 서버: 입력 정보 유효성 검증 (이메일/닉네임 중복 체크 포함)
   - 서버: 사용자 정보를 **인증 대기 상태(`email_verified=false`)**로 저장
   - 서버: 6자리 난수 인증 코드 생성 후 Redis에 저장 (TTL 5분)
   - 서버: 해당 이메일로 인증 코드 발송
   - Response: `201 Created` + `{ "message": "인증 메일이 발송되었습니다." }`

2. **인증 코드 확인 (`POST /api/v1/auth/signup/verify`)**
   - 사용자가 수신한 6자리 인증 코드 입력
   - Request: `{ "email": "user@example.com", "code": "123456" }`
   - 서버: Redis에서 코드 조회 및 검증
     - 코드 불일치: `400 Bad Request` ("인증 코드가 일치하지 않습니다.")
     - 코드 만료: `400 Bad Request` ("인증 코드가 만료되었습니다.")
   - 검증 성공 시:
     - `users.email_verified = true` 업데이트
     - Redis 키 삭제 (일회용)
     - JWT 토큰 발급 (로그인 처리)
   - Response: `200 OK` + `{ "userId": 123, "accessToken": "...", "refreshToken": "..." }`

3. **인증 코드 재발송 (`POST /api/v1/auth/signup/resend`)**
   - 인증 대기 상태(`email_verified=false`)인 사용자만 요청 가능
   - Request: `{ "email": "user@example.com" }`
   - 기존 인증 코드 삭제 후 새 코드 생성 및 발송
   - **Rate Limit**: 1분당 1회 제한 (스팸 방지)
   - Response: `200 OK` + `{ "message": "인증 메일이 재발송되었습니다." }`

##### 인증 대기 상태 관리
- `email_verified=false`인 사용자도 **로그인 가능**
  - 로그인 응답에 `emailVerified: false` 포함
  - 클라이언트에서 인증 화면으로 유도 (선택적)
- 인증 대기 상태로 **24시간 경과** 시 자동 삭제 (스케줄러)
- 동일 이메일로 재가입 시도 시:
  - 인증 대기 상태 사용자 존재 → 기존 데이터 삭제 후 새로 가입 진행

##### 로그인 응답 변경
모든 로그인/토큰 재발급 API 응답에 `emailVerified` 필드 추가:
```json
{
  "userId": 123,
  "accessToken": "eyJhbG...",
  "refreshToken": "eyJhbG...",
  "emailVerified": false
}
```

##### 데이터 모델 변경
```sql
-- users 테이블에 컬럼 추가
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN email_verified_at TIMESTAMP;
```

##### Redis 키 구조
- **Key**: `signup:verification:{email}`
- **Value**: `{ "code": "123456", "userId": 123 }`
- **TTL**: 5분 (300초)

##### 정책 요약
| 항목 | 값 |
|------|-----|
| 인증 코드 길이 | 6자리 숫자 |
| 인증 코드 유효 시간 | 5분 |
| 인증 코드 재발송 제한 | 1분당 1회 |
| 인증 대기 상태 유지 기간 | 24시간 |
| 미인증 사용자 로그인 | 허용 (응답에 `emailVerified: false` 포함) |

### 3.2 User Management
- 내 정보 조회·수정 (닉네임, 학교, 전공, 프로필 사진, 자기소개, 언어)
- **내 정보 조회 응답에 `userId` 필드 포함** (User.id 값, v2.5.10)
- **내 정보 조회 응답에 `userRole` 필드 포함** (`USER`, `ADMIN`, `MANAGER`)
- 이메일 유저만 비밀번호 변경 가능
- 회원 탈퇴 → soft delete (deleted_at)

#### 3.2.1 대학/전공 설정
- **대학 목록 조회**: 검색어(keyword) 및 지역(region) 필터 지원
- **지역 목록 조회**: 대학 필터링용 지역 목록 제공
- **전공 목록 조회**: 대학별 전공 목록, 검색어 필터 지원
- **대학/전공 설정** (`PATCH /api/v1/users/me/university`):
  - 전공 유효성 검증: 선택한 전공이 해당 대학에 속하는지 확인
  - 전공은 선택사항 (`majorId` nullable)
  - 대학 변경 시 기존 전공은 무효화될 수 있음

### 3.3 University Verification (대학생 인증) – v2 신규 핵심 기능
| 방식                | 설명                                      | 자동/수동 | is_verified |
|---------------------|-------------------------------------------|-----------|-------------|
| 학교 웹메일 인증    | 학교 이메일 입력 → 인증 코드 발송 → 코드 검증 | 자동      | true        |
| 학생증 사진 인증     | 사진 업로드(Multipart) → 관리자 수동 검토    | 수동      | true        |
| 수동 승인 (운영자)   | 특수 케이스                               | 수동      | true        |

#### 3.3.1 학교 웹메일 인증 프로세스 (신규 변경)
1. **인증 요청 (`POST /api/v1/verification/email/request`)**
   - 사용자가 학교 웹메일 주소 입력 (예: `user@univ.ac.kr`)
   - 서버: 이메일 도메인으로 학교 식별 (`universities` 테이블의 도메인 정보 매칭)
   - 서버: 6자리 난수 인증 코드 생성 및 해당 이메일로 발송
   - **정책:** 인증 코드는 **발급 후 5분간만 유효**하며, 시간 경과 시 **자동으로 만료(삭제)**되어야 함 (Redis TTL 활용).
2. **코드 검증 (`POST /api/v1/verification/email/confirm`)**
   - 사용자가 수신한 인증 코드 입력
   - 서버: 코드 일치 여부 및 유효 시간 검증
     - 시간 만료 시: `400 Bad Request` (Time expired) 반환
   - **정책:** 인증 성공 시 보안을 위해 해당 코드는 **즉시 파기**되어 재사용할 수 없음.
   - 성공 시:
     - `users.university_id` 업데이트
     - `users.is_verified = true` 업데이트
     - `users.verification_method = EMAIL` 업데이트

### 3.4 Community & Boards

#### 3.4.1 계층 구조
```
Community (커뮤니티)
  └── Board (게시판)
        └── Post (게시글)
              └── Comment (댓글/대댓글)
```

#### 3.4.2 커뮤니티 종류
| 종류 | university_id | 접근 권한 |
|------|---------------|----------|
| **공통 커뮤니티** | `NULL` | 모든 사용자 (미인증 포함) |
| **대학교 커뮤니티** | 대학 ID | 해당 대학 인증 사용자만 (`is_verified=true`) |

#### 3.4.3 게시판 관리 (관리자 전용)
- **생성** (`POST /api/v1/communities/{id}/boards`): 관리자(ADMIN, MANAGER)만 가능
- **수정** (`PATCH /api/v1/boards/{id}`): 관리자만 가능
- **삭제** (`DELETE /api/v1/boards/{id}`): 관리자만 가능
- **게시판 필드**: name(이름), type(타입), description(설명), is_secret(비밀글 전용 여부)
- **게시판 타입** (`board_type`): `GENERAL`, `NOTICE`, `QNA`, `ANONYMOUS`

#### 3.4.4 접근 제어
게시판 목록 조회(`GET /api/v1/communities/{id}/boards`) 시:
1. **공통 커뮤니티**: 무조건 허용
2. **대학교 커뮤니티**: 사용자의 `university_id`가 일치하고 `is_verified=true`여야 함
3. 조건 불만족 시 `403 Forbidden` 반환

### 3.5 Posts & Comments
- 제목(150자), 내용, 다중 이미지, 익명/비밀글 옵션
- 좋아요 토글 (soft delete 방식)
- 계층형 댓글 (최대 2단계: 댓글 → 대댓글)
- **댓글 수정:** 내용 및 익명 여부 수정 가능 (작성자 본인만)
- 댓글에도 이미지 첨부 가능
- 신고·조회수·삭제(soft delete)
- **writerId 필드**: 게시글/댓글 응답에 작성자 ID(`writerId`) 포함 (v2.5.10)
- **isMine 필드**: 게시글/댓글 조회 시 현재 로그인한 유저가 작성자인지 여부를 `isMine` 필드로 반환 (수정/삭제 버튼 표시 판단용)

#### 3.5.1 댓글 타입 (`comment_type`)
| 타입 | 설명 | depth |
|------|------|-------|
| `GENERAL` | 최상위 댓글 | 0 |
| `ANONYMOUS` | 대댓글 (상위 댓글에 대한 답글) | 1 |

> **Note:** 프론트엔드 호환성을 위해 enum 값이 `COMMENT/REPLY`에서 `GENERAL/ANONYMOUS`로 변경됨 (v2.2)

### 3.6 Friends, Block & User Report

#### 3.6.1 친구 관리
- 친구 요청 → 대기/수락/거절
- 친구 목록 조회, 친구 삭제

#### 3.6.2 사용자 차단
사용자 차단은 친구 관계와 독립적으로 동작합니다.

- **차단 (`POST /api/v1/users/{userId}/block`)**
  - 어디서든 (프로필, 채팅, 게시글 작성자 등) 특정 사용자를 차단
  - 차단 시 효과:
    - 새 1:1 채팅방 생성 불가
    - 기존 채팅방은 유지되나 새 메시지 전송 시 `403 Forbidden`
    - 상대방의 게시글/댓글이 목록에서 숨김 처리 (선택적)
    - 상대방이 나를 친구 추가 불가
- **차단 해제 (`DELETE /api/v1/users/{userId}/block`)**
- **차단 목록 조회 (`GET /api/v1/users/me/blocked`)**

#### 3.6.3 사용자 신고
콘텐츠(게시글/댓글) 신고와 별개로, 사용자 자체를 신고하는 기능입니다.

- **사용자 신고 (`POST /api/v1/users/{userId}/report`)**
  - 신고 사유: `SPAM`, `HARASSMENT`, `INAPPROPRIATE_PROFILE`, `IMPERSONATION`, `OTHER`
  - 신고 시 상세 내용(description) 입력 가능
  - 동일 사용자에 대한 중복 신고 방지 (일정 기간 내)
- **신고 context**: 어디서 신고했는지 기록
  - `PROFILE`: 프로필 페이지에서 신고
  - `CHAT`: 채팅 중 신고
  - `POST`: 게시글 작성자 신고
  - `COMMENT`: 댓글 작성자 신고

#### 3.6.4 채팅 메시지 신고 (선택적)
특정 채팅 메시지를 신고하는 기능입니다.

- **메시지 신고 (`POST /api/v1/chat/rooms/{roomId}/messages/{messageId}/report`)**
  - 신고 사유: `SPAM`, `HARASSMENT`, `INAPPROPRIATE_CONTENT`, `OTHER`
  - 신고된 메시지 내용이 Admin에게 전달됨

### 3.7 Chat (1:1 + 그룹 실시간 채팅)

#### 3.7.1 채팅방 생성
- **1:1 채팅**: `POST /api/v1/chat/rooms`
  - 요청: `{ "targetUserId": 999 }`
  - 로직: 서버는 (userA, userB) 정규화하여 기존 방 조회. 있으면 기존 roomId 반환, 없으면 생성.
- **그룹 채팅**: `POST /api/v1/chat/group-rooms`
  - 요청: `{ "roomName": "과제 스터디", "userIds": [101, 102, 103] }`
  - 로직: 새로운 그룹 채팅방 생성 후, 요청자를 포함한 모든 참여자 초대.

#### 3.7.2 실시간 채팅 (Kafka 기반 v2.2)
- **Client → Server:** STOMP over WebSocket (`/ws/v1/chat`)
- **Server → Kafka:** `chat-messages` 토픽으로 메시지 발행 (Produce)
- **Kafka → Server:** Consumer가 메시지 구독 (Consume) → DB 저장 및 WebSocket으로 브로드캐스팅

##### 데이터 흐름
1. **Client:** `/pub/chat/message` (destination)으로 메시지 전송 (STOMP)
   - Payload: `{ "roomId": 123, "content": "Hello" }`
2. **Backend (WebSocketController):** 메시지 수신 후 즉시 `ChatKafkaProducer` 호출
3. **ChatKafkaProducer:** 메시지를 `chat-messages` 토픽으로 직렬화하여 전송
4. **ChatKafkaConsumer:**
   - `chat-messages` 토픽 구독
   - 메시지 수신 후 DB에 저장 (`messages` 테이블)
   - `/sub/chat/room/{roomId}` (topic)으로 메시지 브로드캐스팅 (STOMP)
5. **Client:** `/sub/chat/room/{roomId}` 구독 중, 메시지 수신하여 화면에 표시

##### WebSocket Endpoint
- `wss://api.nonstop.app/ws/v1/chat`
- 연결 시 Access Token 쿼리 파라미터로 인증

##### 장점
- **확장성:** 채팅 서버(WebSocket 세션 보유)와 메시지 처리 로직(DB 저장) 분리
- **안정성:** Kafka가 메시지 브로커 역할, 트래픽 급증에도 안정적 처리
- **메시지 유실 방지:** Consumer 장애 시에도 Kafka에 메시지 보관

##### 설계 고려사항 및 상세 구현 가이드

1.  **메시지 순서 보장 (Message Ordering)**
    *   **방안:** `chat-messages` 토픽으로 메시지 발행 시, **`roomId`를 Kafka 메시지 키(Key)로 설정**하여 전송합니다.
    *   **효과:** 동일 `roomId` 메시지는 항상 동일 파티션에 할당되어 Consumer가 순서대로 메시지를 처리, 채팅 메시지의 전송 순서가 보장됩니다.

2.  **멱등성 및 중복 방지 (Idempotency)**
    *   **방안:**
        *   클라이언트는 메시지 전송 시 **`clientMessageId` (UUID)**를 생성하여 Payload에 포함합니다. `ChatKafkaProducer`는 이를 Kafka 메시지에 포함하고, `ChatKafkaConsumer`는 메시지 수신 후 DB 저장 시 `clientMessageId`를 함께 저장합니다.
        *   Kafka Producer 설정: `enable.idempotence=true`를 활성화하고, 트랜잭셔널 Producer를 사용하여 원자적인 쓰기 작업을 보장합니다.
        *   Kafka Consumer 설정: `isolation.level=read_committed`를 설정하여, 트랜잭션이 커밋된 메시지만 읽도록 합니다.
    *   **효과:** Kafka의 exactly-once semantics를 클라이언트(`clientMessageId`) 및 Kafka 레벨에서(`enable.idempotence`, 트랜잭셔널 Producer/Consumer) 강화하여 네트워크 재시도로 인한 중복을 완벽하게 방지하고, Consumer의 DB 중복 체크 부담을 줄여 DB 부하를 감소시킵니다.

3.  **읽음 처리 (Unread Count) 전략 고도화**
    *   **방안:** 사용자가 채팅방에 진입하거나 메시지를 수신하는 시점에 '읽음' 이벤트를 별도의 Kafka 토픽(`chat-read-events`)으로 발행합니다. 이 토픽을 구독하는 전용 Consumer가 `last_read_message_id` 및 `unread_count` 업데이트를 처리합니다. (선택적으로 Kafka Streams 또는 KTable을 활용하여 `unread_count`를 실시간으로 집계하는 방안도 고려할 수 있습니다.)
    *   **효과:** 읽음 처리와 같은 빈번한 DB 쓰기 작업을 메인 메시지 처리 흐름과 분리하여 비동기로 처리함으로써, 채팅 메시지 전송 속도에 영향을 주지 않고 시스템 성능을 최적화할 수 있습니다. **Consumer lag 모니터링은 필수입니다. lag이 커지면 unread count 반영 지연이 발생할 수 있습니다.**

4.  **그룹 채팅 초대 및 퇴장 이벤트 처리**
    *   **방안:** 그룹 채팅의 `INVITE`, `LEAVE`, `KICK` 등 시스템 관련 이벤트도 일반 채팅 메시지와 동일하게 Kafka를 통해 처리합니다. 이들 이벤트는 특별한 `messageType`을 가지는 시스템 메시지로 정의합니다.
    *   **데이터 흐름 예시 (`INVITE`):**
        1.  `POST /api/v1/chat/group-rooms/{roomId}/invite` API 호출.
        2.  Backend는 초대 비즈니스 로직 수행 (DB 내 유저-방 매핑 추가 등).
        3.  `chat-messages` 토픽으로 `type: INVITE` (시스템 메시지 타입), `content: "OOO님이 초대되었습니다."` 등의 Payload를 가진 메시지 발행.
        4.  `ChatKafkaConsumer`가 이 메시지를 받아 해당 방 모든 유저에게 브로드캐스팅 및 DB 저장.

5.  **채팅 이미지 전송 워크플로우 (Azure SAS URL 연동)**
    *   **방안:** 채팅 메시지에 이미지를 포함할 경우, 클라이언트는 이미지 파일을 Azure Blob Storage에 직접 업로드(SAS URL 사용)하고, **`upload-complete` API 호출을 통해 서버에 파일 메타데이터 저장을 완료한 이후에** 해당 `blobUrl`을 포함한 실제 채팅 메시지를 `/pub/chat/message` (STOMP)로 전송하도록 클라이언트 로직을 설계합니다.
    *   **효과:** 상대방이 메시지를 수신했을 때 유효한 이미지 경로를 즉시 확인할 수 있도록 보장하며, 서버 부담을 줄입니다.

##### Kafka 클러스터 운영 가이드
> 상세 설정은 [채팅 시스템 통합 설계서](./chatting-docs/chat-system.md#4-kafka-설정) 참조

- **토픽 구성**: `chat-messages` (파티션 10, roomId 키), `chat-read-events` (파티션 5, userId 키)
- **Retention**: `chat-messages` 7일, `chat-read-events` 1일
- **모니터링**: Consumer lag, throughput, partition balance (Prometheus + Grafana)
- **에러 핸들링**: Dead Letter Topic (DLQ) – `chat-messages-dlt`, `chat-read-events-dlt`
- **보안**: SASL/SSL + ACL 설정

##### 향후 개선 및 대안 고려 (선택적)
- **과도한 복잡도 완화**: 초기 및 중기 트래픽의 대학생 앱 규모에서는 Kafka가 과도한 복잡도를 야기할 수 있습니다.
  - **대안**: Redis Streams/PubSub 또는 NATS JetStream과 같은 경량 솔루션이 더 간단하고 비용 효율적일 수 있습니다. (예: Redis는 초저지연, unread 관리 용이, Azure Cache for Redis로 관리형 서비스 활용 가능; NATS JetStream은 lightweight, 고성능, Kafka보다 운영 용이).
  - **제언**: 현재 Kafka 설계를 유지하되, 만약 초기 운영 비용이나 복잡도 관리가 주요 고려사항이 될 경우, **v3 단계에서 Redis Streams/PubSub 등으로의 마이그레이션을 고려**할 수 있습니다. 이는 스케일 필요 시 Kafka의 강점을 활용하면서도, 초기 복잡도를 낮추는 유연성을 제공합니다.


#### 3.7.3 메시지 나에게만 삭제 (카카오톡식)
DELETE /api/v1/chat/rooms/{roomId}/messages/{messageId}

#### 3.7.4 읽음 처리
last_read_message_id + unread_count 자동 관리

### 3.8 Timetable

#### 3.8.1 학기 관리
- **학기 목록 조회 (`GET /api/v1/semesters`)**
  - 사용자의 `university_id`에 해당하는 학기 목록 반환
  - `university_id`가 null인 경우 빈 배열 반환
  - **학기 타입**: `FIRST` (1학기), `SECOND` (2학기), `SUMMER` (여름학기), `WINTER` (겨울학기)

#### 3.8.2 시간표 관리 (CRUD)
- **생성 (`POST /api/v1/timetables`)**
  - 학기당 1개의 시간표만 생성 가능 (DB Unique 제약: `user_id`, `semester_id`)
  - 중복 시 `400 Bad Request`: "이미 해당 학기의 시간표가 존재합니다."
  - Request Body: `{ semesterId, title, isPublic }`

- **목록 조회 (`GET /api/v1/timetables`)**
  - 본인의 시간표 목록만 조회 (수업 항목 미포함)

- **상세 조회 (`GET /api/v1/timetables/{id}`)**
  - 본인 시간표: 항상 조회 가능
  - 타인 시간표: `is_public=true`인 경우에만 조회 가능
  - 비공개 타인 시간표 접근 시 `403 Forbidden`: "비공개 시간표는 본인만 조회할 수 있습니다."
  - Response: 시간표 정보 + 수업 항목 목록 포함

- **수정 (`PATCH /api/v1/timetables/{id}`)**
  - 본인의 시간표만 수정 가능 (소유권 검증)
  - 수정 가능 필드: `title`, `isPublic`
  - 권한 없음 시 `403 Forbidden`: "본인의 시간표에만 수정할 수 있습니다."

- **삭제 (`DELETE /api/v1/timetables/{id}`)**
  - 본인의 시간표만 삭제 가능 (소유권 검증)
  - 삭제 시 관련 수업 항목(Entry)도 함께 삭제
  - 권한 없음 시 `403 Forbidden`: "본인의 시간표에만 삭제할 수 있습니다."

#### 3.8.3 수업 항목(Entry) 관리
- **추가 (`POST /api/v1/timetables/{id}/entries`)**
  - 본인의 시간표에만 추가 가능
  - **시간 중복 검증 (Overlap Check)**: 같은 요일에 기존 수업과 시간이 겹치면 `400 Bad Request`
    - 에러 메시지: "수업 시간이 겹칩니다: {과목명} ({시작시간} ~ {종료시간})"
  - Request Body: `{ subjectName, professor, dayOfWeek, startTime, endTime, place, color }`
  - **dayOfWeek**: `MONDAY`, `TUESDAY`, `WEDNESDAY`, `THURSDAY`, `FRIDAY`, `SATURDAY`, `SUNDAY`

- **수정 (`PATCH /api/v1/timetables/entries/{id}`)**
  - 본인의 시간표에 속한 수업만 수정 가능
  - 수정 시에도 시간 중복 검증 수행 (자기 자신 제외)

- **삭제 (`DELETE /api/v1/timetables/entries/{id}`)**
  - 본인의 시간표에 속한 수업만 삭제 가능

#### 3.8.4 공개 시간표 조회
- **엔드포인트**: `GET /api/v1/timetables/public`
- **접근 제어**:
  - `university_id`가 null인 경우 → `403 Forbidden`
  - `is_verified=false`인 경우 → `403 Forbidden`
  - 에러 메시지: "대학 인증이 완료된 사용자만 공개 시간표를 조회할 수 있습니다."
- **로직**: 동일한 `university_id`를 가진 사용자 중 `is_public=true`로 설정된 시간표 목록 반환

### 3.9 Notifications & Push
- FCM 사용, 서버에서만 푸시 트리거
- 알림 생성 시 actor의 nickname 스냅샷 저장 (탈퇴·닉변 대비)
- 인앱 알림 목록 + 개별/전체 읽음 처리

### 3.10 Rate Limit & Security
- 모든 쓰기 API: 사용자당 분당 60회 제한

#### 3.10.1 이미지 업로드 전략
- **일반 파일 (게시글/채팅/프로필)**: 클라이언트가 서버로부터 SAS URL을 발급받아 Azure Blob Storage에 직접 업로드합니다. (서버 부하 감소)
- **보안 파일 (학생증 인증)**: 클라이언트가 `Multipart/form-data`로 서버에 전송하고, 서버가 검증 후 Azure Blob Storage에 업로드합니다.

##### 데이터 흐름 (SAS URL 방식)
1.  **SAS URL 요청 (Client → Server)**
    - 클라이언트는 `POST /api/v1/files/sas-url` 엔드포인트로 업로드할 파일의 정보(`fileName`, `contentType`, `purpose`, `targetId` 등)를 전송하여 업로드 권한이 담긴 일회성 URL(SAS URL)을 요청합니다.
    - `purpose`는 `PROFILE_IMAGE`, `BOARD_ATTACHMENT` 등 파일의 사용 목적을 나타내는 Enum입니다.
2.  **파일 직접 업로드 (Client → Azure)**
    - 클라이언트는 서버로부터 받은 SAS URL을 사용하여 파일을 Azure Blob Storage에 직접 업로드(HTTP PUT)합니다.
3.  **업로드 완료 알림 (Client → Server)**
    - 업로드 성공 후, 클라이언트는 `POST /api/v1/files/upload-complete` 엔드포인트로 업로드된 파일의 최종 경로(`blobUrl`)와 원본 파일명, `purpose` 등을 다시 서버에 알려줍니다.
    - 서버는 이 정보를 받아 파일 메타데이터를 `files` 테이블에 저장하고, `purpose`에 따라 사용자 프로필 이미지 URL을 업데이트하는 등의 후속 처리를 수행합니다.

### 3.11 Admin Features (Mobile App Integrated)
별도의 웹 관리자 페이지 없이, 앱 내에서 관리자(ADMIN, MANAGER) 권한을 가진 사용자가 접근할 수 있는 관리 기능을 제공합니다.
**Note: v2.5.6 기준 Backend API 구현 완료되었습니다. (Frontend 미구현)**

#### 3.11.1 대학생 인증 관리 (학생증)
- **인증 요청 목록 조회 (`GET /api/v1/admin/verification/requests`)**
  - 필터: 처리 상태 (`PENDING`, `ACCEPTED`, `REJECTED`), 페이징 지원
  - `PENDING` 상태인 요청을 우선적으로 노출
- **인증 요청 상세 조회 (`GET /api/v1/admin/verification/requests/{id}`)**
  - 학생증 이미지, 요청자 정보, 요청 시간 확인
- **인증 처리 (`POST /api/v1/admin/verification/requests/{id}/process`)**
  - 승인(`ACCEPTED`): 유저의 `is_verified=true`, `university_id` 확정
  - 반려(`REJECTED`): 반려 사유 입력 가능

#### 3.11.2 커뮤니티 및 게시판 관리
- **커뮤니티 목록 조회**: 기존 API 활용하되 관리자용 뷰 필요 시 확장
- **게시판 생성 (`POST /api/v1/communities/{id}/boards`)**
  - 커뮤니티 내 새로운 게시판 개설
  - 설정: 이름, 설명, 타입(일반/익명/공지 등), 익명 허용 여부
- **게시판 수정 (`PATCH /api/v1/boards/{id}`)**
  - 이름, 설명, 상태(활성/비활성) 수정
- **게시판 비활성화 (`DELETE /api/v1/boards/{id}`)**
  - Soft Delete 처리 (목록에서 숨김)

#### 3.11.3 신고 관리
신고 대상은 **콘텐츠**(게시글, 댓글, 채팅 메시지)와 **사용자**로 구분됩니다.

##### 신고 대상 타입 (`targetType`)
| 타입 | 설명 | 처리 액션 |
|------|------|----------|
| `POST` | 게시글 신고 | 게시글 BLIND/DELETE |
| `COMMENT` | 댓글 신고 | 댓글 BLIND/DELETE |
| `CHAT_MESSAGE` | 채팅 메시지 신고 | 메시지 BLIND/DELETE |
| `USER` | 사용자 신고 | 사용자 경고/정지/차단 |

##### API 명세
- **신고 목록 조회 (`GET /api/v1/admin/reports`)**
  - 필터: 대상(`POST`, `COMMENT`, `CHAT_MESSAGE`, `USER`), 처리 상태(`PENDING`, `RESOLVED`), 페이징
  - **응답 데이터 필수 항목 (네비게이션 지원)**:
    - `id`: 신고 ID
    - `targetType`: 신고 대상 타입
    - `targetId`: 신고된 대상의 ID (게시글/댓글/메시지/사용자 ID)
    - `relatedPostId`: 이동할 게시글 ID (게시글/댓글 신고 시)
    - `relatedRoomId`: 이동할 채팅방 ID (채팅 메시지 신고 시)
    - `content`: 신고된 내용 미리보기
    - `reason`: 신고 사유 (`SPAM`, `HARASSMENT`, `INAPPROPRIATE_CONTENT`, `IMPERSONATION`, `OTHER`)
    - `context`: 신고 발생 위치 (`PROFILE`, `CHAT`, `POST`, `COMMENT`)
    - `reporterName`: 신고자 닉네임 (운영 판단용)
    - `createdAt`: 신고 시각
- **신고 처리 (`POST /api/v1/admin/reports/{id}/process`)**
  - **콘텐츠 신고 처리**:
    - `BLIND`: 신고된 게시글/댓글/메시지 숨김 처리
    - `DELETE`: 신고된 콘텐츠 삭제
    - `REJECT`: 신고 기각, 콘텐츠 유지
  - **사용자 신고 처리**:
    - `WARNING`: 경고 (누적 관리)
    - `SUSPEND`: 일시 정지 (기간 설정)
    - `BAN`: 영구 차단
    - `REJECT`: 신고 기각
  - 처리 시 신고 상태를 `RESOLVED`로 변경

## 4. API Endpoint Summary – Golden Master v2.5.5 (완전 목록)

### Authentication
| Method | URI                                    | Description                     |
|--------|----------------------------------------|---------------------------------|
| POST   | /api/v1/auth/signup                    | 이메일 회원가입 (인증 대기 상태) |
| POST   | /api/v1/auth/signup/verify             | 회원가입 이메일 인증 코드 확인   |
| POST   | /api/v1/auth/signup/resend             | 회원가입 인증 코드 재발송        |
| POST   | /api/v1/auth/login                     | 이메일 로그인                   |
| POST   | /api/v1/auth/google                    | Google 로그인                   |
| POST   | /api/v1/auth/refresh                   | Access Token 재발급             |
| POST   | /api/v1/auth/logout                    | Refresh Token 무효화               |
| GET    | /api/v1/auth/email/check               | 이메일 중복 체크                |
| GET    | /api/v1/auth/nickname/check            | 닉네임 중복 체크                |

### Admin (Backend Implemented, Frontend Pending)
| Method | URI                                             | Description                                      |
|--------|-------------------------------------------------|--------------------------------------------------|
| GET    | /api/v1/admin/verifications                     | 학생증 인증 요청 목록 (Paging)                   |
| POST   | /api/v1/admin/verifications/{id}/approve        | 학생증 인증 승인                                 |
| POST   | /api/v1/admin/verifications/{id}/reject         | 학생증 인증 반려                                 |
| GET    | /api/v1/admin/reports                           | 신고 목록 조회 (Paging)                          |
| POST   | /api/v1/admin/reports/{id}/process              | 신고 처리 (BLIND/REJECT)                         |
| GET    | /api/v1/admin/users                             | 사용자 목록 조회 (Search, Paging)                |
| PATCH  | /api/v1/admin/users/{id}/role                   | 사용자 권한 변경 (USER/ADMIN)                    |
| PATCH  | /api/v1/admin/users/{id}/status                 | 사용자 상태 변경 (활성/비활성)                   |
| POST   | /api/v1/communities/{id}/boards                 | 게시판 생성 (관리자 전용)                        |
| PATCH  | /api/v1/boards/{id}                             | 게시판 수정 (관리자 전용)                        |
| DELETE | /api/v1/boards/{id}                             | 게시판 비활성화 (관리자 전용)                    |

### User & Device
| Method | URI                                       | Description                              |
|--------|-------------------------------------------|------------------------------------------|
| GET    | /api/v1/users/me                          | 내 정보 조회                             |
| PATCH  | /api/v1/users/me                          | 프로필 수정 (학교·전공·닉네임·사진·언어) |
| PATCH  | /api/v1/users/me/password                 | 비밀번호 변경                            |
| DELETE | /api/v1/users/me                          | 회원 탈퇴                                |
| POST   | /api/v1/devices/fcm-token                 | FCM 토큰 등록·갱신 (upsert)              |
| GET    | /api/v1/users/me/verification-status      | 인증 상태 조회 (v2 신규)                 |
| POST   | /api/v1/verification/student-id           | 학생증 사진 업로드 인증 요청 (v2 신규)   |
| GET    | /api/v1/users/me/agreements               | 약관 동의 내역 조회                      |
| PATCH  | /api/v1/users/me/agreements               | 약관 동의 변경 (선택 항목)               |
| GET    | /api/v1/terms/{type}                      | 약관 문서 조회 (최신 버전)               |

### University
| Method | URI                                   | Description                              |
|--------|---------------------------------------|------------------------------------------|
| GET    | /api/v1/universities                  | 대학 목록 (검색, 지역 필터 지원)         |
| GET    | /api/v1/universities/list             | 대학 목록 (회원가입용, 인증 불필요, 페이징 지원) |
| GET    | /api/v1/universities/{id}             | 대학 상세 조회                           |
| GET    | /api/v1/universities/{id}/majors      | 전공 목록 (검색 지원)                    |
| GET    | /api/v1/universities/regions          | 지역 목록 조회 (필터용, 인증 불필요)     |
| PATCH  | /api/v1/users/me/university           | 대학/전공 설정 (전공 유효성 검증 포함)   |

### Community & Board
| Method | URI                                          | Description                              |
|--------|----------------------------------------------|------------------------------------------|
| GET    | /api/v1/communities                          | 커뮤니티 목록                            |
| GET    | /api/v1/communities/{id}/boards              | 게시판 목록 (공통 또는 본인 학교 & 인증) |
| POST   | /api/v1/communities/{id}/boards              | 게시판 생성 (관리자 전용)                |
| PATCH  | /api/v1/boards/{id}                          | 게시판 수정 (관리자 전용)                |
| DELETE | /api/v1/boards/{id}                          | 게시판 삭제 (관리자 전용)                |

### Post & Comment
| Method | URI                                          | Description           |
|--------|----------------------------------------------|-----------------------|
| GET    | /api/v1/boards/{boardId}/posts               | 게시글 목록           |
| POST   | /api/v1/boards/{boardId}/posts               | 게시글 작성           |
| GET    | /api/v1/posts/{postId}                       | 게시글 상세           |
| PATCH  | /api/v1/posts/{postId}                       | 게시글 수정           |
| DELETE | /api/v1/posts/{postId}                       | 게시글 삭제           |
| POST   | /api/v1/posts/{postId}/like                  | 좋아요 토글           |
| POST   | /api/v1/posts/{postId}/report                | 게시글 신고           |
| GET    | /api/v1/posts/{postId}/comments              | 댓글 목록             |
| POST   | /api/v1/posts/{postId}/comments              | 댓글·대댓글 작성      |
| PATCH  | /api/v1/comments/{commentId}                 | 댓글 수정             |
| DELETE | /api/v1/comments/{commentId}                 | 댓글 삭제             |
| POST   | /api/v1/comments/{commentId}/like            | 댓글 좋아요 토글      |
| POST   | /api/v1/comments/{commentId}/report        | 댓글 신고             |

### Friend, Block & User Report
| Method | URI                                       | Description                        |
|--------|-------------------------------------------|------------------------------------|
| GET    | /api/v1/friends                           | 친구 목록                          |
| GET    | /api/v1/friends/requests                  | 받은 친구 요청                     |
| POST   | /api/v1/friends/request                   | 친구 요청                          |
| POST   | /api/v1/friends/requests/{id}/accept      | 수락                               |
| POST   | /api/v1/friends/requests/{id}/reject      | 거절                               |
| DELETE | /api/v1/friends/requests/{id}             | 요청 취소                          |
| POST   | /api/v1/users/{userId}/block              | 사용자 차단                        |
| DELETE | /api/v1/users/{userId}/block              | 차단 해제                          |
| GET    | /api/v1/users/me/blocked                  | 차단 목록 조회                     |
| POST   | /api/v1/users/{userId}/report             | 사용자 신고                        |

### Chat
| Method | URI                                                | Description                                |
|:-------|:---------------------------------------------------|:-------------------------------------------|
| GET    | /api/v1/chat/rooms                                 | 내 채팅방 목록 (1:1, 그룹 포함)            |
| POST   | /api/v1/chat/rooms                                 | 1:1 채팅방 생성 (with targetUserId)        |
| DELETE | /api/v1/chat/rooms/{roomId}                        | 채팅방 나가기 (1:1, 그룹 공통)             |
| GET    | /api/v1/chat/rooms/{roomId}/messages               | 과거 메시지 조회 (Pagination)              |
| DELETE | /api/v1/chat/rooms/{roomId}/messages/{msgId}       | 나에게만 메시지 삭제                       |
| POST   | /api/v1/chat/group-rooms                           | 그룹 채팅방 생성                           |
| PATCH  | /api/v1/chat/group-rooms/{roomId}                  | 그룹 채팅방 정보 수정 (이름 등)            |
| GET    | /api/v1/chat/group-rooms/{roomId}/members          | 그룹 채팅방 참여자 목록 조회               |
| POST   | /api/v1/chat/group-rooms/{roomId}/invite           | 그룹 채팅방에 사용자 초대                  |
| DELETE | /api/v1/chat/group-rooms/{roomId}/members/{userId} | 그룹 채팅방에서 사용자 강퇴 (방장 권한)    |
| POST   | /api/v1/chat/rooms/{roomId}/messages/{msgId}/report | 채팅 메시지 신고                          |
| WS     | wss://api.nonstop.app/ws/v1/chat                   | 실시간 채팅 연결 (STOMP Handshake)         |
| SUB    | /sub/chat/room/{roomId}                            | (STOMP) 채팅방 메시지 구독                 |
| PUB    | /pub/chat/message                                  | (STOMP) 메시지 발행 (전송)                 |

### Timetable
| Method | URI                                    | Description                                      |
|--------|----------------------------------------|--------------------------------------------------|
| GET    | /api/v1/semesters                      | 학기 목록 (사용자 대학 기준)                     |
| GET    | /api/v1/timetables                     | 내 시간표 목록 (수업 항목 미포함)                |
| POST   | /api/v1/timetables                     | 시간표 생성 (학기당 1개 제한)                    |
| GET    | /api/v1/timetables/{id}                | 시간표 상세 (수업 항목 포함, 본인/공개만)        |
| PATCH  | /api/v1/timetables/{id}                | 시간표 제목·공개여부 수정 (본인만)               |
| DELETE | /api/v1/timetables/{id}                | 시간표 삭제 (본인만, 수업 항목 함께 삭제)        |
| POST   | /api/v1/timetables/{id}/entries        | 수업 추가 (본인만, 시간 중복 검증)               |
| PATCH  | /api/v1/timetables/entries/{id}        | 수업 수정 (본인만, 시간 중복 검증)               |
| DELETE | /api/v1/timetables/entries/{id}        | 수업 삭제 (본인만)                               |
| GET    | /api/v1/timetables/public              | 공개 시간표 목록 (같은 학교, 인증 사용자만)      |

### Notification
| Method | URI                                    | Description           |
|--------|----------------------------------------|-----------------------|
| GET    | /api/v1/notifications                  | 알림 목록             |
| PATCH  | /api/v1/notifications/{id}/read        | 개별 읽음             |
| PATCH  | /api/v1/notifications/read-all         | 전체 읽음             |

### File
| Method | URI                                    | Description                     |
|--------|----------------------------------------|---------------------------------|
| POST   | /api/v1/files/sas-url                  | 파일 직접 업로드를 위한 SAS URL 요청 |
| POST   | /api/v1/files/upload-complete          | 파일 업로드 완료 콜백             |

---

## 5. Backend Implementation Status (v2.5.5)

### 5.1 Overview
| Feature Domain | Implementation Status | Note |
|---|---|---|
| **Authentication** | ✅ Fully Implemented | JWT, Refresh Token, Auto Login, OAuth (Google) |
| **User & Device** | ✅ Fully Implemented | Profile, FCM Token, `universityId` nullable support |
| **University** | ✅ Fully Implemented | Search, Paging (`/list`), Major validation |
| **Verification** | ✅ Fully Implemented | Webmail (Code), Student ID (Upload), Status Check |
| **Community** | ✅ Fully Implemented | Post/Comment CRUD, Like, `isMine` field, Infinite Scroll |
| **Board (Admin)** | ❌ Not Implemented | Create/Edit/Delete Board endpoints missing |
| **Admin (App)** | ✅ Fully Implemented | Verification Review, Report Management, User Control implemented |
| **Chat** | ✅ Fully Implemented | WebSocket + Kafka, 1:1, Group, Image (SAS) |
| **Timetable** | ✅ Fully Implemented | CRUD, Color, Validation (Overlap), Public View |
| **Report** | ✅ Fully Implemented | Post/Comment Report (Creation only) |
| **File** | ✅ Fully Implemented | Real Azure Blob Integration (SAS URL + Single Container) |
| **Notification** | ✅ Fully Implemented | FCM Push Logic (NotificationService + DeviceService) |

### 5.2 Detailed Verification Notes (2026-01-17)

#### Community & Board
- **Verified:** `CommunityController`, `PostController`, `CommentController` exist and function as expected.
- **Verified:** `isMine` field in `PostResponseDto` and `CommentResponseDto` (via MyBatis `CASE WHEN`).
- **Verified:** `Board.description` field exists.
- **Verified:** `CommentType` uses `GENERAL/ANONYMOUS`.
- **Missing:** Admin-only Board management APIs (`POST/PATCH/DELETE /boards`) are not present in the codebase.

#### University Verification
- **Verified:** `VerificationController` handles both `student-id` (multipart) and `email` (request/confirm).
- **Verified:** `VerificationService` implements logic for redis-based code validation (inferred) and database updates.
- **Missing:** Admin endpoints to list pending requests and approve/reject them.

#### Admin Features (New)
- **Verified:** `AdminController` provides endpoints for verification review (`/verifications`), report processing (`/reports`), and user management (`/users`).
- **Verified:** `AdminService` implements business logic for approving/rejecting verifications and blinding reported content.
- **Verified:** `SecurityConfig` restricts access to `/api/v1/admin/**` to users with `ADMIN` authority.

#### Chat System
- **Verified:** `ChatKafkaProducer` and `ChatKafkaConsumer` classes exist, confirming the Kafka-based architecture.
- **Verified:** `WebSocketChatController` handles STOMP messages.
- **Verified:** `MessageType` includes `IMAGE`, `SYSTEM` types.

#### Timetable
- **Verified:** `TimetableController` provides full CRUD.
- **Verified:** `TimetableService` implements overlap validation (`validateNoTimeOverlap`) and ownership checks.
- **Verified:** `DayOfWeek` enum matches spec.

#### Report
- **Verified:** `ReportController` provides `/posts/{postId}/report` and `/comments/{commentId}/report`.
- **Missing:** Admin capabilities to view and act on these reports.

#### Notification
- **Verified:** `NotificationController` provides list/read/read-all endpoints.
- **Verified:** `NotificationService` calls `FirebaseMessaging` to send multicast push notifications using tokens from `DeviceService`.

#### File Upload
- **Verified:** `FileController` provides SAS URL generation and completion callback.
- **Verified:** `AzureBlobStorageConfig` uses environment variables for real integration.
- **Verified:** `FileService` generates real Azure SAS URLs with 10-min write permission, using a single container (`nonstop`) and purpose-based prefixes.

### 5.3 변경 이력

| 버전 | 날짜 | 변경 내용 |
|------|------|----------|
| v2.5.12 | 2026-01-21 | 회원가입 이메일 인증 기능 명세 추가 (signup/verify, signup/resend API) |
| v2.5.11 | 2026-01-21 | Auth 커스텀 예외 추가 (401/409 응답), Google 로그인 프로필 동기화, RefreshToken soft-revoke 구현 |
| v2.5.10 | 2026-01-21 | 로그인 응답에 `userId` 추가, UserResponseDto에 `userId` 추가, Post/Comment 응답에 `writerId` 추가 |
| v2.5.9 | 2026-01-20 | 회원가입 시 약관 동의 기능 명세 추가 (Terms & Consent) |
| v2.5.8 | 2026-01-20 | 사용자 신고/차단, 채팅 메시지 신고 기능 명세 추가 |
| v2.5.7 | 2026-01-19 | User API: `/api/v1/users/me` 응답에 `userRole` 필드 추가 |
| v2.5.6 | 2026-01-18 | Backend Progress: Admin 모듈 (인증/신고/유저 관리) 구현 완료 |
| v2.5.5 | 2026-01-17 | Backend Progress: Azure Blob Storage (SAS URL) 실제 연동 완료 |
| v2.5.4 | 2026-01-17 | Backend Progress: Notification (FCM Push) 구현 완료 반영 |
| v2.5.3 | 2026-01-17 | Backend Progress Update: Chat API 경로 정규화 반영 (/api/v1/chat/group-rooms) |
| v2.5.2 | 2026-01-17 | Codebase Verification 완료 (Chat/Kafka, Timetable, Report, University Verified) |
| v2.5 | 2026-01-17 | 게시글/댓글 DTO에 isMine 필드 추가 (작성자 본인 여부 판단용) |
| v2.4 | 2026-01-16 | 대학교 목록 조회 API 추가 (회원가입용, 인증 불필요, 페이징 지원) |
| v2.3 | 2026-01-16 | 웹메일 인증 기능 추가 (email/request, email/confirm) |
| v2.2 | 2026-01-16 | CommentType enum 변경 (COMMENT/REPLY → GENERAL/ANONYMOUS) |
| v2.2 | 2026-01-15 | Board.description 필드 추가, 공통 로깅 설정 추가 |
| v2.1 | 2025-12-20 | 초기 버전 |

---

## 6. Post-MVP Roadmap

MVP 이후 단계에서 검토할 기능들입니다.

### 6.1 학년/학기 정보 기반 시간표 추천 시스템

#### 배경
대학교 수업은 학년별 권장 이수 체계가 있습니다 (예: 1학년 교양필수, 2학년 전공기초, 3-4학년 전공심화).
사용자의 학년 정보를 활용하면 시간표 구성 시 적합한 수업을 추천할 수 있습니다.

#### 제안 기능
| 기능 | 설명 | 우선순위 |
|------|------|----------|
| **학년별 수업 추천** | 사용자 학년에 맞는 권장 과목 추천 | P1 |
| **수강 이력 기반 추천** | 이전 학기 수강 과목 기반 다음 과목 제안 | P2 |
| **졸업요건 트래킹** | 필수 이수 학점/과목 충족 여부 시각화 | P3 |

#### 데이터 모델 변경 (안)

**users 테이블 확장**
```sql
ALTER TABLE users ADD COLUMN enrollment_year SMALLINT;      -- 입학년도 (예: 2023)
ALTER TABLE users ADD COLUMN grade_override SMALLINT;       -- 학년 수동 보정 (휴학/편입 대응)
ALTER TABLE users ADD COLUMN academic_status VARCHAR(20);   -- 학적 상태 (ENROLLED, ON_LEAVE, GRADUATED)
```

**학년 계산 로직**
- 기본: `현재연도 - enrollment_year + 1`
- `grade_override`가 있으면 해당 값 우선 사용
- 최대 학년 제한: 대학별 설정 (보통 4년제 → 4, 전문대 → 2)

**수업 메타데이터 테이블 (신규)**
```sql
CREATE TABLE course_metadata (
  id BIGSERIAL PRIMARY KEY,
  university_id BIGINT NOT NULL,
  course_name VARCHAR(100) NOT NULL,
  recommended_grade SMALLINT,           -- 권장 학년 (1, 2, 3, 4)
  course_type VARCHAR(20),              -- REQUIRED, ELECTIVE, MAJOR_REQUIRED 등
  credits SMALLINT,
  created_at TIMESTAMP DEFAULT now()
);
```

#### 구현 고려사항

1. **학년 정보 수집 방식**
   - 학생증 인증 시: 학번에서 입학년도 자동 추출 (예: `2023XXXXX` → 2023)
   - 미인증 사용자: 프로필에서 직접 입력 (선택사항)

2. **휴학/복학/편입 처리**
   - `grade_override` 필드로 실제 학년과 계산 학년 차이 보정
   - `academic_status`로 현재 재학 상태 관리

3. **수업 데이터 확보**
   - Phase 1: 사용자가 직접 입력한 시간표 데이터 집계 (크라우드소싱)
   - Phase 2: 대학별 공식 커리큘럼 데이터 연동 (파트너십 필요)

4. **추천 알고리즘**
   - 초기: 단순 학년 매칭 + 같은 학과 사용자들의 수강 패턴 분석
   - 고도화: 협업 필터링 기반 개인화 추천

#### API 명세 (예정)
| Method | URI | Description |
|--------|-----|-------------|
| PATCH | /api/v1/users/me/academic-info | 학년/학적 정보 설정 |
| GET | /api/v1/courses/recommended | 학년 기반 수업 추천 목록 |
| GET | /api/v1/users/me/graduation-status | 졸업요건 충족 현황 (P3) |

#### 의존성 및 선행 조건
- 시간표 기능 MVP 안정화 완료 후 진행
- 수업 메타데이터 수집 전략 확정 필요
- 대학별 커리큘럼 구조 차이 조사 필요

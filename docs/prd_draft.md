# Nonstop App – Product Requirements Document
**Golden Master v2.1 (2025.12 최종 실서비스 반영 버전 )**

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

#### 3.1.2 지원 로그인 방식
- 이메일 + 비밀번호 (bcrypt)
- Google OAuth 2.0 (모바일 SDK → credential → 백엔드 검증)

#### 3.1.3 Access Token Payload (표준)
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

#### 3.1.4 universityId = null 허용 정책 (Graceful Degradation)
가능 기능: 프로필, 친구, 1:1 채팅, 알림, 내 시간표 관리, **공통 커뮤니티 이용**  
제한 기능 (universityRequired = true 반환):
- **학교별** 커뮤니티/게시판 이용
- 공개 시간표 조회/공개
- 일부 익명 게시판 (운영 정책에 따라)

### 3.2 User Management
- 내 정보 조회·수정 (닉네임, 학교, 전공, 프로필 사진, 자기소개, 언어)
- 이메일 유저만 비밀번호 변경 가능
- 회원 탈퇴 → soft delete (deleted_at)

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
   - 서버: 6자리 난수 인증 코드 생성 및 해당 이메일로 발송 (유효시간 5분)
2. **코드 검증 (`POST /api/v1/verification/email/confirm`)**
   - 사용자가 수신한 인증 코드 입력
   - 서버: 코드 일치 여부 및 유효시간 검증
   - 성공 시:
     - `users.university_id` 업데이트
     - `users.is_verified = true` 업데이트
     - `users.verification_method = EMAIL` 업데이트

### 3.4 Community & Boards
학교별 커뮤니티 및 전역 공통 커뮤니티 → 게시판 계층 구조  
- **공통 커뮤니티:** `university_id = null`. 모든 사용자(미인증 포함)가 접근 가능.
- **학교 커뮤니티:** 사용자의 `university_id`와 일치하는 커뮤니티만 노출. 인증(`is_verified=true`) 필수.

**보안:** 게시판 목록 조회(`GET /api/v1/communities/{id}/boards`) 시:
1. 해당 커뮤니티가 **공통 커뮤니티**인 경우: 무조건 허용.
2. 해당 커뮤니티가 **학교 커뮤니티**인 경우: 사용자의 `university_id`가 커뮤니티의 대학과 일치해야 하며, `is_verified=true`여야 함.
조건 불만족 시 403 Forbidden 반환.

### 3.5 Posts & Comments
- 제목(150자), 내용, 다중 이미지, 익명/비밀글 옵션
- 좋아요 토글 (soft delete 방식)
- 계층형 댓글 (최대 2단계 대댓글 권장, 3단계 이상 차단)
- **댓글 수정:** 내용 및 익명 여부 수정 가능 (작성자 본인만)
- 댓글에도 이미지 첨부 가능
- 신고·조회수·삭제(soft delete)

### 3.6 Friends & Block
- 친구 요청 → 대기/수락/거절/차단
- 차단 시: 새 1:1 채팅방 생성 불가, 기존 채팅방은 유지되나 새 메시지 전송 403

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
- **학기별 시간표 관리 (CRUD)**
  - **생성 (`POST`)**: 학기당 1개의 시간표만 생성 가능 (Unique 제약: `user_id`, `semester_id`).
  - **수정/삭제 (`PATCH`, `DELETE`)**: 본인의 시간표만 수정/삭제 가능 (소유권 검증 필수).
  - **목록/상세 조회 (`GET`)**: 본인의 시간표 목록 및 상세 조회.
- **수업 항목(Entry) 관리**
  - **추가/수정 (`POST`, `PATCH`)**: 시간표 내 기존 수업과 시간이 겹치는지 검증 필수 (Overlap Check).
  - **권한**: 본인의 시간표에만 수업 항목 추가/수정/삭제 가능.
- **공개 시간표 조회 (`GET /api/v1/timetables/public`)**
  - **보안**: 요청자의 `university_id`가 null 이거나 `is_verified=false` 인 경우 `403 Forbidden` 반환.
  - **로직**: 동일한 학교 소속 사용자 중 `is_public=true`로 설정된 시간표 목록 조회.

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

## 4. API Endpoint Summary – Golden Master v2.1 (완전 목록)

### Authentication
| Method | URI                                    | Description                     |
|--------|----------------------------------------|---------------------------------|
| POST   | /api/v1/auth/signup                    | 이메일 회원가입                  |
| POST   | /api/v1/auth/login                     | 이메일 로그인                   |
| POST   | /api/v1/auth/google                    | Google 로그인                   |
| POST   | /api/v1/auth/refresh                   | Access Token 재발급             |
| POST   | /api/v1/auth/logout                    | Refresh Token 무효화               |
| GET    | /api/v1/auth/email/check               | 이메일 중복 체크                |
| GET    | /api/v1/auth/nickname/check            | 닉네임 중복 체크                |

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

### University
| Method | URI                                   | Description   |
|--------|---------------------------------------|---------------|
| GET    | /api/v1/universities                  | 대학 목록     |
| GET    | /api/v1/universities/{id}/majors      | 전공 목록     |

### Community & Board
| Method | URI                                          | Description      |
|--------|----------------------------------------------|------------------|
| GET    | /api/v1/communities                          | 커뮤니티 목록    |
| GET    | /api/v1/communities/{id}/boards              | 게시판 목록 (공통 또는 본인 학교 & 인증) |

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

### Friend & Block
| Method | URI                                       | Description         |
|--------|-------------------------------------------|---------------------|
| GET    | /api/v1/friends                           | 친구 목록           |
| GET    | /api/v1/friends/requests                  | 받은 친구 요청      |
| POST   | /api/v1/friends/request                   | 친구 요청           |
| POST   | /api/v1/friends/requests/{id}/accept      | 수락                |
| POST   | /api/v1/friends/requests/{id}/reject      | 거절                |
| DELETE | /api/v1/friends/requests/{id}             | 요청 취소           |
| POST   | /api/v1/friends/block                     | 차단                |

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
| WS     | wss://api.nonstop.app/ws/v1/chat                   | 실시간 채팅 연결 (STOMP Handshake)         |
| SUB    | /sub/chat/room/{roomId}                            | (STOMP) 채팅방 메시지 구독                 |
| PUB    | /pub/chat/message                                  | (STOMP) 메시지 발행 (전송)                 |

### Timetable
| Method | URI                                    | Description                     |
|--------|----------------------------------------|---------------------------------|
| GET    | /api/v1/semesters                      | 학기 목록                       |
| GET    | /api/v1/timetables                     | 내 시간표 목록                  |
| POST   | /api/v1/timetables                     | 시간표 생성                     |
| GET    | /api/v1/timetables/{id}                | 시간표 상세                     |
| PATCH  | /api/v1/timetables/{id}                | 시간표 제목·공개여부 수정       |
| DELETE | /api/v1/timetables/{id}                | 시간표 삭제                     |
| POST   | /api/v1/timetables/{id}/entries        | 수업 추가                       |
| PATCH  | /api/v1/timetables/entries/{id}        | 수업 수정                       |
| DELETE | /api/v1/timetables/entries/{id}        | 수업 삭제                       |
| GET    | /api/v1/timetables/public              | 공개 시간표 목록 (같은 학교만)  |

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

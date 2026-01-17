# Nonstop App: Golden Master Readiness Report (Updated)
**Date:** 2026.01.17
**Version:** v2.5.1
**Status:** Backend (v2.5.1 main) vs Frontend (dev branch)

---

## 1. 종합 요약 (Executive Summary)

| 구분 | 이전 평가 | 현재 평가 | 변경 사유 |
|:---|:---:|:---:|:---|
| **Backend** | 90% | **80%** | Admin 기능 전무, Notification FCM 미구현, File 서비스 Mock 상태 |
| **Frontend** | 60% | **55%** | Friends 모듈 완전 부재, Timetable API 구조 불일치, Notification 전무 |

### 핵심 블로커 (Critical Blockers)
1. **[Frontend] Friends 모듈 완전 부재**: `features/friends/` 디렉토리에 `.gitkeep` 파일만 존재
2. **[Frontend] Timetable API 구조 불일치**: Frontend는 Event 중심, Backend는 Timetable/Entry 중심
3. **[Frontend] Notification 모듈 전무**: FCM 연동 및 알림 목록 화면 없음
4. **[Frontend] Email Verification API 미연결**: UI만 존재, API 호출 코드 없음
5. **[Backend] Admin 기능 전무**: 학생증 심사, 신고 관리 API 없음

---

## 2. 도메인별 상세 진척도 분석

### 2.1 Authentication & Verification (인증)

| 항목 | Backend | Frontend | 연동 상태 | 상세 분석 |
|:---|:---:|:---:|:---:|:---|
| **로그인** | ✅ 100% | ✅ 100% | ✅ 완료 | JWT Access/Refresh Token 정상 발급. `AuthApiImpl.signIn()` 완벽 구현 |
| **회원가입** | ✅ 100% | ✅ 100% | ✅ 완료 | University/Major 선택 포함. 가입 후 자동 로그인 처리 |
| **로그아웃** | ✅ 100% | ✅ 100% | ✅ 완료 | Refresh Token 무효화 + 로컬 토큰 삭제 |
| **Google OAuth** | ✅ 100% | ⚠️ 50% | ⚠️ 부분 | Backend 준비 완료, Frontend `GoogleSignInButton` 존재하나 연동 미검증 |
| **이메일 인증** | ✅ 100% | ❌ 0% | ❌ 미연동 | **`EmailVerificationScreen`에 API 호출 로직 전무. `verifyEmail()` throws UnimplementedError** |
| **학생증 인증** | ✅ 90% | ❌ 0% | ❌ 미연동 | Backend에 `VerificationController` 존재. Frontend 화면/로직 미구현 |
| **이메일 중복확인** | ✅ 100% | ✅ 100% | ✅ 완료 | `/api/v1/auth/email/check` 연동됨 |
| **닉네임 중복확인** | ✅ 100% | ✅ 100% | ✅ 완료 | `/api/v1/auth/nickname/check` 연동됨 |
| **토큰 갱신** | ✅ 100% | ⚠️ 70% | ⚠️ 검증필요 | `DioClient` interceptor에 refresh 로직 존재하나 edge case 테스트 필요 |

**Backend 상세:**
- `AuthController`: login, signup, logout, refresh, email/nickname check 완비
- `VerificationController`: email 인증 발송/검증, 학생증 업로드/상태조회 완비
- JWT 설정: Access 30분, Refresh 30일

**Frontend 상세:**
- `auth_api_impl.dart:139-148`: `verifyEmail`, `resendEmailVerification` 모두 `throw UnimplementedError`
- `email_verification_screen.dart`: Verify 버튼 클릭 시 API 호출 없이 바로 `/onboarding`으로 이동

---

### 2.2 User Profile (사용자 프로필)

| 항목 | Backend | Frontend | 연동 상태 | 상세 분석 |
|:---|:---:|:---:|:---:|:---|
| **내 정보 조회** | ✅ 100% | ✅ 100% | ✅ 완료 | `/api/v1/users/me` 연동됨 |
| **프로필 수정** | ✅ 100% | ⚠️ 70% | ⚠️ 검증필요 | `updateProfile()` 구현됨. introduction 필드명 검증 필요 |
| **비밀번호 변경** | ✅ 100% | ❌ 0% | ❌ 미구현 | Backend `PATCH /users/me/password` 존재. Frontend 미구현 |
| **계정 삭제** | ✅ 100% | ✅ 100% | ⚠️ 검증필요 | `DELETE /api/v1/users/me` 구현됨 |
| **프로필 이미지** | ✅ 100% | ⚠️ 50% | ⚠️ 검증필요 | Backend SAS URL 방식. Frontend 이미지 업로드 로직 검증 필요 |

**Backend 상세:**
- `UserController`: getMe, updateProfile, changePassword, deleteAccount
- `UserDto`: id, email, nickname, universityName, majorName, introduction, profileImageUrl, createdAt

---

### 2.3 University & Major (대학/전공)

| 항목 | Backend | Frontend | 연동 상태 | 상세 분석 |
|:---|:---:|:---:|:---:|:---|
| **대학 목록 조회** | ✅ 100% | ✅ 100% | ✅ 완료 | 페이지네이션 + 검색 지원 |
| **전공 목록 조회** | ✅ 100% | ✅ 100% | ✅ 완료 | `universityId` 기반 조회 |

**Backend 상세:**
- `UniversityController`: `GET /universities` (paging, search), `GET /universities/{id}/majors`
- `UniversityDto`, `MajorDto` 완비

---

### 2.4 Community & Board (커뮤니티/게시판)

| 항목 | Backend | Frontend | 연동 상태 | 상세 분석 |
|:---|:---:|:---:|:---:|:---|
| **커뮤니티 목록** | ✅ 100% | ✅ 100% | ✅ 완료 | `/api/v1/communities` 연동됨 |
| **게시판 목록** | ✅ 100% | ✅ 100% | ✅ 완료 | `/api/v1/communities/{id}/boards` 연동됨 |
| **게시글 목록** | ✅ 100% | ✅ 100% | ✅ 완료 | 페이지네이션 지원. `board_remote_data_source.dart` 완비 |
| **게시글 상세** | ✅ 100% | ✅ 100% | ✅ 완료 | 조회수 증가 포함 |
| **게시글 작성** | ✅ 100% | ✅ 100% | ✅ 완료 | 익명/비밀글 + 이미지 URL 지원 |
| **게시글 수정** | ✅ 100% | ✅ 100% | ✅ 완료 | `PATCH /api/v1/posts/{id}` |
| **게시글 삭제** | ✅ 100% | ✅ 100% | ✅ 완료 | Soft delete |
| **게시글 좋아요** | ✅ 100% | ✅ 100% | ✅ 완료 | Toggle 방식 |
| **isMine 필드** | ✅ 100% | ✅ 100% | ✅ 완료 | v2.5에서 추가됨 |

**Backend 상세:**
- `CommunityController`, `BoardController`, `PostController` 완비
- `PostDto`: id, boardId, authorNickname, title, content, viewCount, likeCount, commentCount, isAnonymous, isSecret, isMine, imageUrls, createdAt, updatedAt
- BoardType ENUM: FREE, SECRET, QUESTION, INFO, MARKET

**Frontend 상세:**
- `BoardRemoteDataSource`: 모든 API 실제 구현 완료
- `PostEntity.fromJson()`: Backend DTO와 정확히 매핑됨

---

### 2.5 Comment (댓글)

| 항목 | Backend | Frontend | 연동 상태 | 상세 분석 |
|:---|:---:|:---:|:---:|:---|
| **댓글 목록** | ✅ 100% | ✅ 100% | ✅ 완료 | 대댓글 구조 (upperCommentId) 지원 |
| **댓글 작성** | ✅ 100% | ✅ 100% | ✅ 완료 | 익명 + 이미지 URL 지원 |
| **댓글 수정** | ✅ 100% | ✅ 100% | ✅ 완료 | |
| **댓글 삭제** | ✅ 100% | ✅ 100% | ✅ 완료 | |
| **댓글 좋아요** | ✅ 100% | ✅ 100% | ✅ 완료 | |
| **isMine 필드** | ✅ 100% | ✅ 100% | ✅ 완료 | v2.5에서 추가됨 |

**Backend 상세:**
- `CommentController`: CRUD + like 완비
- `CommentDto`: id, postId, authorNickname, content, likeCount, isAnonymous, isMine, upperCommentId, imageUrls, createdAt, updatedAt

---

### 2.6 Friends (친구) - CRITICAL

| 항목 | Backend | Frontend | 연동 상태 | 상세 분석 |
|:---|:---:|:---:|:---:|:---|
| **친구 목록** | ✅ 100% | ❌ 0% | ❌ 미구현 | **`features/friends/` 디렉토리에 .gitkeep만 존재** |
| **친구 요청 보내기** | ✅ 100% | ❌ 0% | ❌ 미구현 | |
| **받은 요청 목록** | ✅ 100% | ❌ 0% | ❌ 미구현 | |
| **요청 수락/거절** | ✅ 100% | ❌ 0% | ❌ 미구현 | |
| **친구 삭제** | ✅ 100% | ❌ 0% | ❌ 미구현 | |
| **차단 목록** | ✅ 100% | ❌ 0% | ❌ 미구현 | |
| **차단/해제** | ✅ 100% | ❌ 0% | ❌ 미구현 | |

**Backend 상세 (완전 구현됨):**
- `FriendController`:
  - `GET /friends` - 친구 목록
  - `GET /friends/requests` - 받은 요청 목록
  - `POST /friends/request` - 요청 보내기
  - `PUT /friends/request/{id}/accept` - 수락
  - `PUT /friends/request/{id}/reject` - 거절
  - `DELETE /friends/{id}` - 친구 삭제
- `BlockController`:
  - `GET /blocks` - 차단 목록
  - `POST /blocks` - 차단하기
  - `DELETE /blocks/{id}` - 차단 해제
- `FriendDto`, `FriendRequestDto`, `BlockDto` 완비
- FriendStatus ENUM: PENDING, ACCEPTED, REJECTED

**Frontend 상세:**
```
lib/features/friends/
├── data/
│   └── .gitkeep          # 빈 파일
├── domain/
│   └── .gitkeep          # 빈 파일
└── presentation/
    └── .gitkeep          # 빈 파일
```
**구현 필요 항목:**
- `FriendApi`, `FriendApiImpl`
- `FriendRepository`, `FriendRepositoryImpl`
- `FriendDto`, `FriendRequestDto`, `BlockDto`
- `FriendsListScreen`, `FriendRequestsScreen`, `BlockedUsersScreen`
- `FriendProvider` (Riverpod)

---

### 2.7 Timetable (시간표) - CRITICAL

| 항목 | Backend | Frontend | 연동 상태 | 상세 분석 |
|:---|:---:|:---:|:---:|:---|
| **시간표 조회** | ✅ 100% | ✅ 100% (Mock) | ❌ 구조불일치 | **API 구조가 완전히 다름** |
| **시간표 생성** | ✅ 100% | ✅ 100% (Mock) | ❌ 구조불일치 | |
| **시간표 수정** | ✅ 100% | ✅ 100% (Mock) | ❌ 구조불일치 | |
| **시간표 삭제** | ✅ 100% | ✅ 100% (Mock) | ❌ 구조불일치 | |
| **수업 항목 추가** | ✅ 100% | ✅ 100% (Mock) | ❌ 구조불일치 | |
| **수업 항목 수정** | ✅ 100% | ✅ 100% (Mock) | ❌ 구조불일치 | |
| **수업 항목 삭제** | ✅ 100% | ✅ 100% (Mock) | ❌ 구조불일치 | |
| **공개 시간표 조회** | ✅ 100% | ❌ 0% | ❌ 미구현 | 친구 시간표 보기 기능 |

**API 구조 불일치 상세:**

| 구분 | Backend 구조 | Frontend 구조 |
|:---|:---|:---|
| **메인 엔티티** | Timetable (시간표 컨테이너) | Event (수업 이벤트) |
| **하위 엔티티** | TimetableEntry (수업 항목) | 없음 |
| **API 패턴** | `/timetables/{id}/entries` | `/events` |
| **시간표 개념** | 학기별 여러 시간표 가능 | 단일 이벤트 목록 |

**Backend API 구조:**
```
GET    /api/v1/timetables              # 내 시간표 목록
POST   /api/v1/timetables              # 시간표 생성 (name, semester, isPublic)
GET    /api/v1/timetables/{id}         # 시간표 상세
PATCH  /api/v1/timetables/{id}         # 시간표 수정
DELETE /api/v1/timetables/{id}         # 시간표 삭제
GET    /api/v1/timetables/{id}/entries # 수업 항목 목록
POST   /api/v1/timetables/{id}/entries # 수업 항목 추가
PATCH  /api/v1/entries/{id}            # 수업 항목 수정
DELETE /api/v1/entries/{id}            # 수업 항목 삭제
GET    /api/v1/users/{id}/timetables   # 타인 공개 시간표
```

**Backend DTO:**
```java
TimetableDto {
  id, userId, name, semester, isPublic, createdAt, updatedAt
}
TimetableEntryDto {
  id, timetableId, courseName, professorName, location,
  dayOfWeek (MONDAY-SUNDAY), startTime, endTime, color
}
```

**Frontend 현재 구조 (Mock):**
```dart
// timetable_api.dart
abstract class TimetableApi {
  Future<List<Event>> getEvents();
  Future<Event> createEvent(Event event);
  Future<Event> updateEvent(Event event);
  Future<void> deleteEvent(String eventId);
}
```

**필요한 Frontend 수정:**
1. `Timetable` 도메인 모델 생성 (시간표 컨테이너)
2. `TimetableEntry` 도메인 모델 생성 (수업 항목)
3. `TimetableApi` 인터페이스 재설계
4. `TimetableApiImpl` 실제 HTTP 구현
5. Repository/Provider 전면 수정
6. UI에서 시간표 선택 -> 수업 목록 구조로 변경

---

### 2.8 Chat (채팅)

| 항목 | Backend | Frontend | 연동 상태 | 상세 분석 |
|:---|:---:|:---:|:---:|:---|
| **채팅방 목록** | ✅ 100% | ✅ 100% | ⚠️ 검증필요 | API 구조 존재, Mock 데이터 사용 중 |
| **채팅방 생성** | ✅ 100% | ✅ 100% | ⚠️ 검증필요 | 1:1 및 그룹 채팅 |
| **채팅방 상세** | ✅ 100% | ✅ 100% | ⚠️ 검증필요 | |
| **메시지 목록** | ✅ 100% | ✅ 100% | ⚠️ 검증필요 | 페이지네이션 지원 |
| **실시간 메시지** | ✅ 100% | ⚠️ 70% | ⚠️ 검증필요 | STOMP 클라이언트 구현됨, Kafka 연동 테스트 필요 |
| **이미지 전송** | ✅ 100% | ❌ 0% | ❌ 미구현 | SAS URL 방식 이미지 업로드 로직 없음 |
| **읽음 처리** | ✅ 100% | ⚠️ 50% | ⚠️ 검증필요 | |
| **채팅방 나가기** | ✅ 100% | ✅ 100% | ⚠️ 검증필요 | |

**Backend 상세:**
- `ChatController`:
  - `GET /api/v1/chat/rooms` - 내 채팅방 목록
  - `POST /api/v1/chat/rooms` - 1:1 채팅방 생성
  - `POST /api/v1/chat/group-rooms` - 그룹 채팅방 생성
  - `GET /api/v1/chat/rooms/{roomId}/messages` - 메시지 목록 (페이지네이션)
  - `PATCH /api/v1/chat/rooms/{roomId}/read` - 읽음 처리
  - `DELETE /api/v1/chat/rooms/{roomId}` - 채팅방 나가기
- WebSocket (STOMP):
  - Subscribe: `/topic/chat/{chatRoomId}`
  - Send: `/app/chat/{chatRoomId}/message`
- Kafka: `chat-messages` 토픽으로 메시지 발행/구독
- `ChatRoomDto`, `ChatMessageDto`, `ChatParticipantDto` 완비
- MessageType ENUM: TEXT, IMAGE, SYSTEM

**Frontend 상세:**
- `StompService`: WebSocket 연결 관리 구현됨 (`stomp_service.dart`)
- `ChatApiImpl` 구조 존재하나 실제 사용처에서 Mock 데이터 반환
- 이미지 메시지 전송 UI/로직 없음

---

### 2.9 Notification (알림) - CRITICAL

| 항목 | Backend | Frontend | 연동 상태 | 상세 분석 |
|:---|:---:|:---:|:---:|:---|
| **알림 목록** | ✅ 100% | ❌ 0% | ❌ 미구현 | **Frontend notification 모듈 전무** |
| **알림 읽음** | ✅ 100% | ❌ 0% | ❌ 미구현 | |
| **FCM Push** | ✅ 100% | ❌ 0% | ❌ 미구현 | Backend FCM 연동 완료. Frontend 수신 로직 필요 |

**Backend 상세:**
- `NotificationController`:
  - `GET /notifications` - 알림 목록
  - `PUT /notifications/{id}/read` - 읽음 처리
  - `PUT /notifications/read-all` - 전체 읽음
- `NotificationService`: FCM MulticastMessage 발송 로직 구현됨 (DeviceService 연동)
- `NotificationDto`: id, userId, type, title, body, data, isRead, createdAt
- NotificationType ENUM: FRIEND_REQUEST, FRIEND_ACCEPTED, NEW_COMMENT, NEW_LIKE, NEW_CHAT_MESSAGE

**Frontend 상태:**
```
lib/features/notification/  # 디렉토리 자체가 존재하지 않음
```

---

### 2.10 Report (신고)

| 항목 | Backend | Frontend | 연동 상태 | 상세 분석 |
|:---|:---:|:---:|:---:|:---|
| **신고 생성** | ✅ 100% | ❌ 0% | ❌ 미연동 | UI에 신고 버튼 있으나 API 호출 없음 |
| **신고 관리 (Admin)** | ❌ 0% | ❌ 0% | ❌ 미구현 | 관리자 기능 전무 |

**Backend 상세:**
- `ReportController`: `POST /reports` (신고 생성만 존재)
- `ReportDto`: id, reporterId, targetType, targetId, reason, status, createdAt
- TargetType ENUM: POST, COMMENT, USER
- ReportStatus ENUM: PENDING, RESOLVED, REJECTED
- **Admin API 미구현**: 신고 목록 조회, 처리(BLIND) 기능 없음

---

### 2.11 File Upload (파일 업로드)

| 항목 | Backend | Frontend | 연동 상태 | 상세 분석 |
|:---|:---:|:---:|:---:|:---|
| **SAS URL 발급** | ⚠️ Mock | ⚠️ 부분 | ⚠️ 검증필요 | Backend Mock 구현, Frontend 일부 구현 |
| **이미지 업로드** | ⚠️ Mock | ⚠️ 부분 | ⚠️ 검증필요 | 게시글 이미지는 URL 전달 방식 |

**Backend 상세:**
- `FileController`: `POST /files/upload-url` - SAS URL 발급
- `AzureStorageService`: Mock 구현 상태 (실제 Azure 연동 필요)

---

### 2.12 Admin (관리자) - NOT IMPLEMENTED

| 항목 | Backend | Frontend | 연동 상태 | 상세 분석 |
|:---|:---:|:---:|:---:|:---|
| **학생증 인증 심사** | ❌ 0% | ❌ 0% | ❌ 미구현 | 심사 목록/승인/반려 API 없음 |
| **신고 관리** | ❌ 0% | ❌ 0% | ❌ 미구현 | 신고 목록/BLIND 처리 API 없음 |
| **사용자 관리** | ❌ 0% | ❌ 0% | ❌ 미구현 | |
| **통계 대시보드** | ❌ 0% | ❌ 0% | ❌ 미구현 | |

---

## 3. API 연동 현황 매트릭스

| 도메인 | Backend API | Frontend API | 연동률 |
|:---|:---:|:---:|:---:|
| Auth | 10/10 | 7/10 | **70%** |
| User | 5/5 | 4/5 | **80%** |
| University | 2/2 | 2/2 | **100%** |
| Community | 2/2 | 2/2 | **100%** |
| Board | 2/2 | 2/2 | **100%** |
| Post | 6/6 | 6/6 | **100%** |
| Comment | 5/5 | 5/5 | **100%** |
| Friend | 7/7 | 0/7 | **0%** |
| Block | 3/3 | 0/3 | **0%** |
| Timetable | 9/9 | 0/9 | **0%** (구조불일치) |
| Chat | 7/7 | 5/7 | **71%** |
| Notification | 3/3 | 0/3 | **0%** |
| Report | 1/1 | 0/1 | **0%** |
| File | 1/1 | 1/1 | **100%** (Mock) |
| **총계** | **63/63** | **34/63** | **54%** |

---

## 4. 남은 구현 목록 (Prioritized Task List)

### Phase 1: Critical Blockers (MVP 필수)

#### 1.1 [Frontend] Friends 모듈 전체 구현
**예상 파일:**
- `lib/features/friends/data/api/friend_api.dart`
- `lib/features/friends/data/api/friend_api_impl.dart`
- `lib/features/friends/data/dto/friend_dto.dart`
- `lib/features/friends/data/dto/friend_request_dto.dart`
- `lib/features/friends/data/dto/block_dto.dart`
- `lib/features/friends/data/repository/friend_repository_impl.dart`
- `lib/features/friends/domain/repository/friend_repository.dart`
- `lib/features/friends/domain/entities/friend.dart`
- `lib/features/friends/presentation/providers/friend_provider.dart`
- `lib/features/friends/presentation/screens/friends_list_screen.dart`
- `lib/features/friends/presentation/screens/friend_requests_screen.dart`
- `lib/features/friends/presentation/screens/blocked_users_screen.dart`
- `lib/features/friends/presentation/widgets/friend_tile.dart`

#### 1.2 [Frontend] Timetable API 구조 재설계 및 실연동
**필요 작업:**
1. `Timetable` 도메인 모델 생성 (id, name, semester, isPublic)
2. `TimetableEntry` 도메인 모델 생성 (courseName, dayOfWeek, startTime, endTime, ...)
3. `TimetableApi` 인터페이스 Backend 구조에 맞게 재설계
4. `TimetableApiImpl` HTTP 클라이언트 구현
5. Repository/Provider 수정
6. UI 시간표 선택 로직 추가

#### 1.3 [Frontend] Email Verification API 연결
**수정 파일:**
- `lib/features/auth/data/api/auth_api_impl.dart`: `verifyEmail()`, `resendEmailVerification()` 구현
- `lib/features/auth/presentation/screens/email_verification_screen.dart`: API 호출 로직 추가

#### 1.4 [Frontend] Notification 모듈 구현
**예상 파일:**
- `lib/features/notification/data/api/notification_api.dart`
- `lib/features/notification/data/api/notification_api_impl.dart`
- `lib/features/notification/data/dto/notification_dto.dart`
- `lib/features/notification/domain/entities/notification.dart`
- `lib/features/notification/presentation/screens/notifications_screen.dart`
- `lib/features/notification/presentation/providers/notification_provider.dart`
- FCM 초기화 및 핸들러 설정

---

### Phase 2: High Priority (기능 완성)

#### 2.1 [Backend] Admin 모듈 구현
**예상 파일:**
- `AdminController`: 학생증 심사 목록/승인/반려
- `AdminReportController`: 신고 목록/BLIND 처리
- `AdminService`, `AdminMapper`

#### 2.2 [Frontend] 학생증 인증 화면 구현
- 이미지 선택 UI
- Multipart 업로드 로직
- 인증 상태 표시

#### 2.3 [Frontend] 채팅 이미지 전송 구현
- 이미지 선택 -> SAS URL 요청 -> 업로드 -> 메시지 전송

#### 2.4 [Frontend] Report API 연결
- 신고 버튼에 API 호출 로직 추가

#### 2.5 [Frontend] 비밀번호 변경 화면 구현

---

### Phase 3: Polish (안정화)

#### 3.1 [Backend] FCM Push 실연동
- Firebase Admin SDK 연동
- 디바이스 토큰 관리

#### 3.2 [Backend] Azure Storage 실연동
- Mock 제거, 실제 SAS URL 발급

#### 3.3 [Common] 통합 테스트
- E2E 시나리오: 회원가입 -> 이메일인증 -> 친구추가 -> 채팅 -> 게시글작성

#### 3.4 [Common] 에러 핸들링 통합
- 일관된 에러 코드 체계
- Frontend 에러 메시지 표시

---

## 5. 기술 스택 요약

### Backend
| 항목 | 기술 |
|:---|:---|
| Framework | Spring Boot 3.x |
| Database | PostgreSQL |
| ORM | MyBatis |
| Auth | JWT (Access 30min, Refresh 30days) |
| Real-time | WebSocket (STOMP) + Kafka |
| Storage | Azure Blob Storage (SAS URL) |
| Push | Firebase Cloud Messaging (미완성) |

### Frontend
| 항목 | 기술 |
|:---|:---|
| Framework | Flutter |
| State | Riverpod |
| HTTP | Dio |
| WebSocket | stomp_dart_client |
| Storage | flutter_secure_storage |
| Architecture | Clean Architecture (Data/Domain/Presentation) |
| Error Handling | fpdart (Either) |

---

## 6. 결론 및 권고사항

### 현재 상태
- **Backend**: 핵심 비즈니스 로직 80% 완성. Admin 기능 추가 필요.
- **Frontend**: UI 프레임워크 구축 완료. API 실연동 55% 수준. Friends/Timetable/Notification 긴급 구현 필요.

### 권고 우선순위
1. **[최우선]** Frontend Friends 모듈 구현 - 앱의 핵심 소셜 기능
2. **[최우선]** Frontend Timetable API 구조 재설계 - Backend와 완전 불일치
3. **[긴급]** Frontend Notification 모듈 구현 - 사용자 경험 필수
4. **[긴급]** Email Verification API 연결 - 회원가입 플로우 완성
5. **[중요]** Backend Admin 기능 - 운영 필수
6. **[중요]** 채팅 이미지 전송 - 채팅 기능 완성

### 예상 Golden Master 도달 조건
- Phase 1 완료 시: MVP 출시 가능 (Backend 85%, Frontend 75%)
- Phase 2 완료 시: 정식 출시 가능 (Backend 95%, Frontend 90%)
- Phase 3 완료 시: 안정화 완료 (Backend 100%, Frontend 100%)

---

*Report Generated: 2026-01-17*
*Analysis Tool: Claude Code*

---

# Nonstop App: Golden Master Readiness Report (Updated)

**Date:** 2026-01-17
**Version:** v2.5.1
**Status:** Backend (v2.5.1 main) vs Frontend (dev branch)

---

## 1. Executive Summary

| Category     | Previous | Current | Reason for Change                                                                     |
| :----------- | :------: | :-----: | :------------------------------------------------------------------------------------ |
| **Backend**  |    90%   | **80%** | No Admin features, FCM Notification incomplete, File service in Mock state            |
| **Frontend** |    60%   | **55%** | Friends module missing, Timetable API structure mismatch, Notification module missing |

### Critical Blockers

1. **[Frontend] Friends module completely missing**: Only `.gitkeep` exists under `features/friends/`
2. **[Frontend] Timetable API structure mismatch**: Frontend is event-based, Backend is timetable/entry-based
3. **[Frontend] Notification module missing**: No FCM integration or notification list UI
4. **[Frontend] Email Verification API not connected**: UI exists, no API call
5. **[Backend] Admin features missing**: No student ID review or report management APIs

---

## 2. Domain-by-Domain Progress Analysis

### 2.1 Authentication & Verification

| Item                           | Backend | Frontend |    Integration   | Details                                          |
| :----------------------------- | :-----: | :------: | :--------------: | :----------------------------------------------- |
| **Login**                      |  ✅ 100% |  ✅ 100%  |      ✅ Done      | JWT Access/Refresh tokens issued correctly       |
| **Sign Up**                    |  ✅ 100% |  ✅ 100%  |      ✅ Done      | Includes University/Major selection              |
| **Logout**                     |  ✅ 100% |  ✅ 100%  |      ✅ Done      | Refresh token invalidation                       |
| **Google OAuth**               |  ✅ 100% |  ⚠️ 50%  |    ⚠️ Partial    | Frontend button exists, integration not verified |
| **Email Verification**         |  ✅ 100% |   ❌ 0%   |  ❌ Not connected | `verifyEmail()` throws `UnimplementedError`      |
| **Student ID Verification**    |  ✅ 90%  |   ❌ 0%   |  ❌ Not connected | Backend implemented, frontend missing            |
| **Email Duplication Check**    |  ✅ 100% |  ✅ 100%  |      ✅ Done      | `/api/v1/auth/email/check`                       |
| **Nickname Duplication Check** |  ✅ 100% |  ✅ 100%  |      ✅ Done      | `/api/v1/auth/nickname/check`                    |
| **Token Refresh**              |  ✅ 100% |  ⚠️ 70%  | ⚠️ Needs testing | Edge cases unverified                            |

**Backend**

* `AuthController`: login, signup, logout, refresh
* `VerificationController`: email & student ID verification
* JWT: Access 30min, Refresh 30 days

**Frontend**

* `auth_api_impl.dart`: email verification methods unimplemented
* `email_verification_screen.dart`: navigates without API call

---

### 2.2 User Profile

| Item                | Backend | Frontend |     Integration     | Details |
| :------------------ | :-----: | :------: | :-----------------: | :------ |
| **Get My Profile**  |  ✅ 100% |  ✅ 100%  |        ✅ Done       |         |
| **Update Profile**  |  ✅ 100% |  ⚠️ 70%  | ⚠️ Needs validation |         |
| **Change Password** |  ✅ 100% |   ❌ 0%   |      ❌ Missing      |         |
| **Delete Account**  |  ✅ 100% |  ✅ 100%  | ⚠️ Needs validation |         |
| **Profile Image**   |  ✅ 100% |  ⚠️ 50%  | ⚠️ Needs validation |         |

---

### 2.3 University & Major

| Item                | Backend | Frontend | Integration |
| :------------------ | :-----: | :------: | :---------: |
| **University List** |  ✅ 100% |  ✅ 100%  |    ✅ Done   |
| **Major List**      |  ✅ 100% |  ✅ 100%  |    ✅ Done   |

---

### 2.4 Community & Board

All community, board, post, and interaction features are **fully implemented and integrated (100%)** on both Backend and Frontend.

---

### 2.5 Comment

All comment-related features (CRUD, likes, nested comments, `isMine`) are **fully implemented and integrated (100%)**.

---

### 2.6 Friends — **CRITICAL**

| Item              | Backend | Frontend | Integration |
| :---------------- | :-----: | :------: | :---------: |
| Friend List       |  ✅ 100% |   ❌ 0%   |      ❌      |
| Send Request      |  ✅ 100% |   ❌ 0%   |      ❌      |
| Incoming Requests |  ✅ 100% |   ❌ 0%   |      ❌      |
| Accept / Reject   |  ✅ 100% |   ❌ 0%   |      ❌      |
| Remove Friend     |  ✅ 100% |   ❌ 0%   |      ❌      |
| Block List        |  ✅ 100% |   ❌ 0%   |      ❌      |
| Block / Unblock   |  ✅ 100% |   ❌ 0%   |      ❌      |

**Frontend status:**
Only empty directories with `.gitkeep` files exist.

---

### 2.7 Timetable — **CRITICAL**

**Major structural mismatch**

| Aspect      | Backend                          | Frontend        |
| :---------- | :------------------------------- | :-------------- |
| Core Entity | Timetable                        | Event           |
| Sub Entity  | TimetableEntry                   | None            |
| API Pattern | `/timetables/{id}/entries`       | `/events`       |
| Concept     | Multiple timetables per semester | Flat event list |

Backend is **fully implemented**, Frontend uses **mock-only, incompatible structure**.

---

### 2.8 Chat

| Item          | Backend | Frontend | Status                       |
| :------------ | :-----: | :------: | :--------------------------- |
| Chat Rooms    |    ✅    |     ✅    | Needs verification           |
| Messages      |    ✅    |     ✅    | Needs verification           |
| Realtime      |    ✅    |    ⚠️    | Kafka/STOMP testing required |
| Image Message |    ✅    |     ❌    | Missing                      |
| Read Receipts |    ✅    |    ⚠️    | Partial                      |

---

### 2.9 Notification — **CRITICAL**

| Item              | Backend | Frontend |
| :---------------- | :-----: | :------: |
| Notification List |    ✅    |     ❌    |
| Mark as Read      |    ✅    |     ❌    |
| FCM Push          |    ✅    |     ❌    |

Frontend has **no notification module at all**. Backend FCM integration is complete.

---

### 2.10 Report

| Item             | Backend | Frontend |
| :--------------- | :-----: | :------: |
| Create Report    |    ✅    |     ❌    |
| Admin Management |    ❌    |     ❌    |

---

### 2.11 File Upload

* Backend: Mock SAS URL implementation
* Frontend: Partial implementation
* Real cloud integration required

---

### 2.12 Admin — **NOT IMPLEMENTED**

All admin-related features (student verification review, report moderation, user management, dashboards) are missing on both sides.

---

## 3. API Integration Matrix

| Domain       |  Backend  |  Frontend | Integration |
| :----------- | :-------: | :-------: | :---------: |
| Auth         |   10/10   |    7/10   |     70%     |
| User         |    5/5    |    4/5    |     80%     |
| University   |    2/2    |    2/2    |     100%    |
| Community    |    100%   |    100%   |     100%    |
| Friend       |    7/7    |    0/7    |      0%     |
| Timetable    |    9/9    |    0/9    |      0%     |
| Notification |    3/3    |    0/3    |      0%     |
| Chat         |    7/7    |    5/7    |     71%     |
| **Total**    | **63/63** | **34/63** |   **54%**   |

---

## 4. Remaining Work (Prioritized)

### Phase 1: MVP Critical

1. Frontend Friends module (full implementation)
2. Timetable API redesign and integration
3. Email verification API connection
4. Notification module + FCM

### Phase 2: High Priority

* Backend Admin module
* Student ID verification UI
* Chat image messages
* Report API integration
* Change password UI

### Phase 3: Polish

* Real FCM integration
* Real Azure Blob Storage
* End-to-End testing
* Unified error handling

---

## 5. Tech Stack Summary

### Backend

* Spring Boot 3.x
* PostgreSQL + MyBatis
* JWT Auth
* WebSocket (STOMP) + Kafka
* Azure Blob Storage (SAS)
* Firebase Cloud Messaging

### Frontend

* Flutter
* Riverpod
* Dio
* STOMP WebSocket
* Clean Architecture
* fpdart (Either)

---

## 6. Conclusion & Recommendation

### Current State

* **Backend**: 80% complete, Admin & infra polish required
* **Frontend**: 55% integrated, core social features missing

### Recommended Priority

1. Friends module
2. Timetable redesign
3. Notification system
4. Email verification
5. Admin features
6. Chat image support

### Golden Master Criteria

* **Phase 1 complete**: MVP release
* **Phase 2 complete**: Production-ready
* **Phase 3 complete**: Fully stabilized

---

*Report Generated: 2026-01-17*
*Analysis Tool: Claude Code*

---

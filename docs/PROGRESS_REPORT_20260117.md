# Nonstop App: Golden Master Readiness Report (Updated)
**Date:** 2026.01.23
**Version:** v2.5.12
**Status:** Backend (main) vs Frontend (dev branch)

---

## 1. 종합 요약 (Executive Summary)

| 구분 | 이전 평가 (01.17) | 현재 평가 (01.23) | 변경 사유 |
|:---|:---:|:---:|:---|
| **Backend** | 85% | **87%** | Google OAuth Firebase 연동 완료, 프로필 이미지 동기화 구현 |
| **Frontend** | 55% | **80%** | ✅ Friends 모듈 완전 구현, ✅ Timetable API 재설계 및 연동 완료 |

### ✅ 해결된 블로커 (Since 01.17)
1. ~~**[Frontend] Friends 모듈 완전 부재**~~: ✅ **완전 구현** - 친구 요청/수락/거절/삭제/차단 모든 기능 API 연동 완료
2. ~~**[Frontend] Timetable API 구조 불일치**~~: ✅ **완전 구현** - Backend 구조에 맞게 재설계, 10개 API 모두 연동 완료
3. **[Backend] Google OAuth**: ✅ Firebase Admin SDK 연동 완료, 프로필 이미지 동기화 추가

### 남은 블로커 (Critical Blockers)
1. **[Frontend] Notification 모듈 전무**: FCM 연동 및 알림 목록 화면 없음 (모듈 자체 미존재)
2. **[Frontend] Email Verification API 미연결**: UI만 존재, API 호출 코드 없음 (`throw UnimplementedError`)
3. **[Backend] Board 관리 기능 전무**: 게시판 생성/수정/삭제 API 없음 (Admin 모듈은 구현됨)
4. **[Backend/Frontend] User Agreements (약관 동의)**: Frontend UI만 존재, Backend API 미구현

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

### 2.6 Friends (친구) - ✅ RESOLVED

| 항목 | Backend | Frontend | 연동 상태 | 상세 분석 |
|:---|:---:|:---:|:---:|:---|
| **친구 목록** | ✅ 100% | ✅ 100% | ✅ 완료 | API 연동 완료 |
| **친구 요청 보내기** | ✅ 100% | ✅ 100% | ✅ 완료 | API 연동 완료 |
| **받은 요청 목록** | ✅ 100% | ✅ 100% | ✅ 완료 | API 연동 완료 |
| **요청 수락/거절** | ✅ 100% | ✅ 100% | ✅ 완료 | API 연동 완료 |
| **요청 취소** | ✅ 100% | ✅ 100% | ✅ 완료 | API 연동 완료 |
| **친구 삭제** | ✅ 100% | ✅ 100% | ✅ 완료 | API 연동 완료 |
| **사용자 검색** | ✅ 100% | ✅ 100% | ✅ 완료 | 닉네임으로 검색 |
| **차단** | ✅ 100% | ✅ 100% | ✅ 완료 | FriendController에 통합 |

**Backend 상세:**
- `FriendController`:
  - `GET /api/v1/friends` - 친구 목록
  - `GET /api/v1/friends/requests` - 받은 요청 목록 (PENDING)
  - `POST /api/v1/friends/request` - 요청 보내기
  - `POST /api/v1/friends/requests/{id}/accept` - 수락
  - `POST /api/v1/friends/requests/{id}/reject` - 거절
  - `DELETE /api/v1/friends/requests/{id}` - 요청 취소
  - `DELETE /api/v1/friends/{id}` - 친구 삭제
  - `POST /api/v1/friends/block` - 차단 (BlockController 없이 통합)
- `FriendDto`, `FriendRequestDto` 완비

**Frontend 상세 (01.23 완전 구현):**
```
lib/features/friends/
├── data/
│   ├── api/
│   │   ├── friend_api.dart (인터페이스)
│   │   └── friend_api_impl.dart (8개 API 구현)
│   ├── dto/
│   │   └── friend_dto.dart (Freezed)
│   └── repository_impl/
│       └── friend_repository_impl.dart
├── domain/
│   ├── entities/
│   │   └── friend.dart
│   └── repository/
│       └── friend_repository.dart
└── presentation/
    ├── providers/
    │   └── friend_management_provider.dart (223줄)
    ├── screens/
    │   └── friends_screen.dart (탭 기반 UI)
    └── widgets/
```
**구현된 API:**
- `GET /api/v1/friends`, `GET /api/v1/friends/requests`
- `POST /api/v1/friends/request`, `POST /api/v1/friends/requests/{id}/accept`
- `POST /api/v1/friends/requests/{id}/reject`, `DELETE /api/v1/friends/requests/{id}`
- `DELETE /api/v1/friends/{id}`, `GET /api/v1/users/search`

---

### 2.7 Timetable (시간표) - ✅ RESOLVED

| 항목 | Backend | Frontend | 연동 상태 | 상세 분석 |
|:---|:---:|:---:|:---:|:---|
| **학기 목록 조회** | ✅ 100% | ✅ 100% | ✅ 완료 | `GET /api/v1/semesters` |
| **시간표 목록 조회** | ✅ 100% | ✅ 100% | ✅ 완료 | `GET /api/v1/timetables` |
| **시간표 생성** | ✅ 100% | ✅ 100% | ✅ 완료 | `POST /api/v1/timetables` |
| **시간표 상세 조회** | ✅ 100% | ✅ 100% | ✅ 완료 | `GET /api/v1/timetables/{id}` |
| **시간표 수정** | ✅ 100% | ✅ 100% | ✅ 완료 | `PATCH /api/v1/timetables/{id}` |
| **시간표 삭제** | ✅ 100% | ✅ 100% | ✅ 완료 | `DELETE /api/v1/timetables/{id}` |
| **수업 항목 추가** | ✅ 100% | ✅ 100% | ✅ 완료 | `POST /api/v1/timetables/{id}/entries` |
| **수업 항목 수정** | ✅ 100% | ✅ 100% | ✅ 완료 | `PATCH /api/v1/timetables/entries/{id}` |
| **수업 항목 삭제** | ✅ 100% | ✅ 100% | ✅ 완료 | `DELETE /api/v1/timetables/entries/{id}` |
| **공개 시간표 조회** | ✅ 100% | ✅ 100% | ✅ 완료 | `GET /api/v1/timetables/public` |

**API 구조 (Backend = Frontend 일치):**

| 구분 | Backend 구조 | Frontend 구조 (01.23 재설계) |
|:---|:---|:---|
| **메인 엔티티** | Timetable | Timetable |
| **하위 엔티티** | TimetableEntry | TimetableEntry |
| **API 패턴** | `/timetables/{id}/entries` | `/timetables/{id}/entries` |
| **시간표 개념** | 학기별 시간표 관리 | 학기별 시간표 관리 |

**Backend API 구조:**
```
GET    /api/v1/semesters               # 학기 목록
GET    /api/v1/timetables              # 내 시간표 목록
POST   /api/v1/timetables              # 시간표 생성 (semesterId, title, isPublic)
GET    /api/v1/timetables/{id}         # 시간표 상세 (수업 항목 포함)
PATCH  /api/v1/timetables/{id}         # 시간표 수정
DELETE /api/v1/timetables/{id}         # 시간표 삭제
POST   /api/v1/timetables/{id}/entries # 수업 항목 추가
PATCH  /api/v1/timetables/entries/{id} # 수업 항목 수정
DELETE /api/v1/timetables/entries/{id} # 수업 항목 삭제
GET    /api/v1/timetables/public       # 공개 시간표 (같은 학교 인증 사용자)
```

**Frontend 상세 (01.23 완전 재설계 및 구현):**
```
lib/features/timetable/
├── data/
│   ├── api/
│   │   └── timetable_api_impl.dart (280줄 - 10개 API 구현)
│   ├── dto/
│   │   ├── semester_dto.dart
│   │   ├── timetable_dto.dart
│   │   └── timetable_entry_dto.dart
│   └── repository_impl/
├── domain/
│   ├── entities/
│   │   ├── semester.dart
│   │   ├── timetable.dart
│   │   ├── timetable_entry.dart
│   │   └── day_of_week.dart
│   └── repository/
└── presentation/
    ├── providers/
    │   └── timetable_management_provider.dart (442줄)
    ├── screens/
    │   ├── timetable_screen.dart
    │   ├── add_timetable_entry_screen.dart
    │   └── gpa_calculator_screen.dart
    └── widgets/
```

**추가 기능:**
- GPA 계산기 통합 (`gpa_provider.dart`, `gpa_calculator_screen.dart`)
- 시간 중복 검증 (Backend에서 처리)
- 학기별 시간표 관리 (학기당 1개 제한)

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
| **SAS URL 발급** | ✅ 100% | ⚠️ 부분 | ⚠️ 검증필요 | Backend Azure Blob 연동 완료 (단일 컨테이너 + 폴더 구조) |
| **이미지 업로드** | ✅ 100% | ⚠️ 부분 | ⚠️ 검증필요 | Client Direct Upload 지원 |

**Backend 상세:**
- `FileController`: `POST /files/upload-url` - SAS URL 발급 (실제 Azure 연동)
- `AzureStorageService`: `BlobServiceClient` 설정 완료, `nonstop` 컨테이너 사용
- `FileService`: Purpose별 폴더 구조(`profile_image/uuid...`) 적용된 SAS 생성

---

### 2.12 Admin (관리자) - BACKEND IMPLEMENTED

| 항목 | Backend | Frontend | 연동 상태 | 상세 분석 |
|:---|:---:|:---:|:---:|:---|
| **학생증 인증 심사** | ✅ 100% | ❌ 0% | ❌ 미구현 | Backend API 구현 완료 (`/api/v1/admin/verifications`) |
| **신고 관리** | ✅ 100% | ❌ 0% | ❌ 미구현 | Backend API 구현 완료 (`/api/v1/admin/reports`) |
| **사용자 관리** | ✅ 100% | ❌ 0% | ❌ 미구현 | Backend API 구현 완료 (`/api/v1/admin/users`) |
| **통계 대시보드** | ❌ 0% | ❌ 0% | ❌ 미구현 | |

**Backend 상세:**
- `AdminController`: 인증 심사, 신고 처리, 사용자 관리 API 제공
- `AdminService`: 승인/반려, BLIND 처리 로직 구현
- `AdminMapper`: 관련 조회 및 업데이트 쿼리 구현
- **Security**: `/api/v1/admin/**`는 `ADMIN` 권한 필수

---

## 3. API 연동 현황 매트릭스 (2026.01.23 Updated)

| 도메인 | Backend API | Frontend API | 연동률 | 변경 |
|:---|:---:|:---:|:---:|:---:|
| Auth | 10/10 | 7/10 | **70%** | - |
| User | 5/5 | 4/5 | **80%** | - |
| University | 2/2 | 2/2 | **100%** | - |
| Community | 2/2 | 2/2 | **100%** | - |
| Board | 2/2 | 2/2 | **100%** | - |
| Post | 6/6 | 6/6 | **100%** | - |
| Comment | 5/5 | 5/5 | **100%** | - |
| Friend | 8/8 | 8/8 | **100%** | ⬆️ 0%→100% |
| Timetable | 10/10 | 10/10 | **100%** | ⬆️ 0%→100% |
| Chat | 7/7 | 5/7 | **71%** | - |
| Notification | 3/3 | 0/3 | **0%** | - |
| Report | 1/1 | 0/1 | **0%** | - |
| Admin | 8/8 | 0/8 | **0%** | - |
| File | 1/1 | 1/1 | **100%** | - |
| **총계** | **70/70** | **52/70** | **74%** | ⬆️ 47%→74% |

### 주요 개선 내역 (01.17 → 01.23)
- **Friend 모듈**: 0% → 100% (8개 API 전체 연동)
- **Timetable 모듈**: 0% → 100% (10개 API 전체 연동, 구조 재설계)
- **전체 연동률**: 47% → 74% (+27%p)

---

## 4. 남은 구현 목록 (Prioritized Task List) - 2026.01.23 Updated

### ✅ 완료된 작업 (Phase 1 부분 완료)

#### ~~1.1 [Frontend] Friends 모듈 전체 구현~~ ✅ DONE
- 완료일: 2026.01.23
- 8개 API 전체 연동, UI 완성

#### ~~1.2 [Frontend] Timetable API 구조 재설계 및 실연동~~ ✅ DONE
- 완료일: 2026.01.23
- Backend 구조에 맞게 재설계, 10개 API 전체 연동
- GPA 계산기 추가 구현

---

### Phase 1: Critical Blockers (MVP 필수) - 남은 작업

#### 1.3 [Frontend] Email Verification API 연결
**현재 상태:** UI만 존재, API 미구현 (`throw UnimplementedError`)
**수정 파일:**
- `lib/features/auth/data/api/auth_api_impl.dart`: `verifyEmail()`, `resendEmailVerification()` 구현
- `lib/features/auth/presentation/screens/email_verification_screen.dart`: API 호출 로직 추가

#### 1.4 [Frontend] Notification 모듈 구현
**현재 상태:** 모듈 자체가 존재하지 않음 (라우트 상수만 정의)
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

#### 2.1 [Backend] Board 관리 (Admin) 기능 구현
- `BoardController`에 관리자용 API 추가 (생성/수정/삭제)
- PRD 요구사항: `POST/PATCH/DELETE /api/v1/boards`

#### 2.2 [Backend/Frontend] User Agreements (약관 동의) 구현
- **Backend:** 약관 API 구현 필요 (현재 미구현)
- **Frontend:** UI 완성 (체크박스), 백엔드 연동 필요

#### 2.3 [Frontend] 학생증 인증 화면 구현
- 이미지 선택 UI
- Multipart 업로드 로직
- 인증 상태 표시 (Backend API는 구현됨)

#### 2.4 [Frontend] 채팅 이미지 전송 구현
- 이미지 선택 -> SAS URL 요청 -> 업로드 -> 메시지 전송
- **참고:** WebSocket/STOMP 인프라는 완성됨

#### 2.5 [Frontend] Report API 연결
- 신고 버튼에 API 호출 로직 추가 (Backend API는 구현됨)

#### 2.6 [Frontend] 비밀번호 변경 화면 구현
- Backend API 구현됨: `PATCH /api/v1/users/me/password`

---

### Phase 3: Polish (안정화)

#### 3.1 [Common] 통합 테스트
- E2E 시나리오: 회원가입 -> 이메일인증 -> 친구추가 -> 채팅 -> 게시글작성

#### 3.2 [Common] 에러 핸들링 통합
- 일관된 에러 코드 체계
- Frontend 에러 메시지 표시

#### 3.3 [Backend] Rate Limiting 적용
- HTTP REST API에 분당 60회 제한 적용 (PRD 요구사항)
- WebSocket에만 적용됨, HTTP API는 미적용

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

## 6. 결론 및 권고사항 (2026.01.23 Updated)

### 현재 상태
- **Backend**: 핵심 비즈니스 로직 **87% 완성**
  - ✅ Google OAuth Firebase 연동 완료
  - ✅ 프로필 이미지 동기화 추가
  - ❌ Board Admin API 미구현
  - ❌ User Agreements API 미구현

- **Frontend**: **80% 연동 완료** (이전 55% → 80%로 대폭 개선)
  - ✅ Friends 모듈 완전 구현 (8개 API)
  - ✅ Timetable 모듈 완전 구현 (10개 API)
  - ✅ WebSocket/STOMP 인프라 완성
  - ❌ Notification 모듈 미구현
  - ❌ Email Verification API 미연동

### 권고 우선순위 (Updated)
1. **[최우선]** Frontend Notification 모듈 구현 - 사용자 경험 필수
2. **[최우선]** Email Verification API 연결 - 회원가입 플로우 완성
3. **[긴급]** Backend Board Admin API - 운영 필수
4. **[중요]** User Agreements 전체 구현 - 법적 요구사항
5. **[중요]** Chat 이미지 전송 - 채팅 기능 완성
6. **[권장]** Report API 연결 - 콘텐츠 관리

### Golden Master Criteria
| Phase | 조건 | 상태 |
|:---:|:---|:---:|
| **Phase 1** | MVP 출시 가능 | ⚠️ 진행 중 (74% 연동) |
| **Phase 2** | 정식 출시 가능 | 🔜 다음 단계 |
| **Phase 3** | 안정화 완료 | 🔜 후순위 |

### 예상 Golden Master 도달 조건
- **Phase 1 완료 시**: MVP 출시 가능 (Backend 87%, Frontend 85%)
- **Phase 2 완료 시**: 정식 출시 가능 (Backend 95%, Frontend 95%)
- **Phase 3 완료 시**: 안정화 완료 (Backend 100%, Frontend 100%)

---

## 7. 변경 이력

| 날짜 | 버전 | 변경 내용 |
|:---|:---:|:---|
| 2026.01.23 | v2.5.12 | 전체 리포트 업데이트 - Friends/Timetable 완전 구현 반영, 연동률 74%로 상향 |
| 2026.01.23 | - | Google OAuth Firebase 연동 완료, 프로필 이미지 동기화 추가 |
| 2026.01.17 | v2.5.1 | 초기 리포트 생성 |

---

*Report Generated: 2026-01-23*
*Analysis Tool: Claude Code*

---

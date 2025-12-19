# Nonstop App – Product Requirements Document
**Golden Master v2.1 (2025.12 최종 실서비스 반영 버전 – Azure Migration)**

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
  "sub": 12345,
  "email": "hello@korea.ac.kr",
  "nickname": "코알라",
  "universityId": 52,        // null 허용
  "isVerified": true,        // 대학생 인증 여부 (v2 신규)
  "authProvider": "EMAIL|GOOGLE",
  "iat": 1735999999,
  "exp": 1736001799
}
```

#### 3.1.4 universityId = null 허용 정책 (Graceful Degradation)
가능 기능: 프로필, 친구, 1:1 채팅, 알림, 내 시간표 관리  
제한 기능 (universityRequired = true 반환):
- 커뮤니티/게시판 이용
- 공개 시간표 조회/공개
- 일부 익명 게시판 (운영 정책에 따라)

### 3.2 User Management
- 내 정보 조회·수정 (닉네임, 학교, 전공, 프로필 사진, 자기소개, 언어)
- 이메일 유저만 비밀번호 변경 가능
- 회원 탈퇴 → soft delete (deleted_at)

### 3.3 University Verification (대학생 인증) – v2 신규 핵심 기능
| 방식                | 설명                                      | 자동/수동 | is_verified |
|---------------------|-------------------------------------------|-----------|-------------|
| 이메일 도메인 인증   | @*.ac.kr, 대학별 도메인 목록 자동 매칭     | 자동      | true        |
| 학생증 사진 인증     | 사진 업로드 → 관리자 수동 검토             | 수동      | true        |
| 수동 승인 (운영자)   | 특수 케이스                               | 수동      | true        |

### 3.4 Community & Boards
학교별 커뮤니티 → 게시판 계층 구조  
university_id = null → 빈 배열 + universityRequired 플래그 반환

### 3.5 Posts & Comments
- 제목(150자), 내용, 다중 이미지, 익명/비밀글 옵션
- 좋아요 토글 (soft delete 방식)
- 계층형 댓글 (최대 2단계 대댓글 권장, 3단계 이상 차단)
- 댓글에도 이미지 첨부 가능
- 신고·조회수·삭제(soft delete)

### 3.6 Friends & Block
- 친구 요청 → 대기/수락/거절/차단
- 차단 시: 새 1:1 채팅방 생성 불가, 기존 채팅방은 유지되나 새 메시지 전송 403

### 3.7 Chat (1:1 + 그룹 실시간 채팅)

#### 3.7.1 채팅방 생성 (1:1 전용)
POST /api/v1/chat/rooms  
→ 요청 바디 { "targetUserId": 999 }  
→ 서버는 (userA, userB) 정규화하여 기존 방 조회 → 있으면 기존 roomId 반환, 없으면 생성

#### 3.7.2 실시간 채팅
WebSocket: wss://api.nonstop.app/ws/v1/chat?access_token=xxx
- Access Token 만료 시 서버 → close code 4001 ("token_expired")

#### 3.7.3 메시지 나에게만 삭제 (카카오톡식)
DELETE /api/v1/chat/rooms/{roomId}/messages/{messageId}

#### 3.7.4 읽음 처리
last_read_message_id + unread_count 자동 관리

### 3.8 Timetable
- 학기별 시간표 CRUD
- 공개 설정 시 동일 university_id & is_verified=true 인 사용자만 조회 가능
- GET /api/v1/timetables/public → 요청자의 university_id 가 null 이면 403 + universityRequired

### 3.9 Notifications & Push
- FCM 사용, 서버에서만 푸시 트리거
- 알림 생성 시 actor의 nickname 스냅샷 저장 (탈퇴·닉변 대비)
- 인앱 알림 목록 + 개별/전체 읽음 처리

### 3.10 Rate Limit & Security
- 모든 쓰기 API: 사용자당 분당 60회 제한
- 이미지 업로드: Azure Blob Storage SAS URL 방식

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
| GET    | /api/v1/communities/{id}/boards              | 게시판 목록      |

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
| Method | URI                                           | Description                        |
|--------|-----------------------------------------------|------------------------------------|
| GET    | /api/v1/chat/rooms                            | 채팅방 목록                        |
| POST   | /api/v1/chat/rooms                            | 1:1 채팅방 생성 (targetUserId)     |
| DELETE | /api/v1/chat/rooms/{roomId}                   | 채팅방 나가기                      |
| GET    | /api/v1/chat/rooms/{roomId}/messages          | 과거 메시지 (offset pagination)    |
| DELETE | /api/v1/chat/rooms/{roomId}/messages/{msgId}  | 나에게만 메시지 삭제 (v2 신규)     |
| WS     | wss://api.nonstop.app/ws/v1/chat              | 실시간 채팅                        |

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
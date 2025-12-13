# Nonstop App - Backend

대학생 전용 실명 기반 커뮤니티 모바일 앱의 백엔드 서버입니다.

## 1. Overview

**"Nonstop"** 은 대학생들을 위한 커뮤니티 앱으로, 학교 인증 없이도 주요 기능을 사용할 수 있으며, 점진적으로 학교 및 학생 인증을 유도하여 신뢰도 높은 커뮤니티를 구축하는 것을 목표로 합니다.

- **Target Users**: 신입생, 재학생
- **Core Journey**:
    1.  신입생 → 학교·전공 선택 → 학생 인증 → 커뮤니티/시간표 이용
    2.  재학생 → 친구·채팅·게시판·시간표 공유 중심

## 2. Core Features

- **Authentication**: 이메일/Google OAuth 2.0 기반의 JWT 인증 (Access/Refresh Token)
- **University Verification**: 이메일 도메인 또는 학생증 사진을 통한 대학생 인증
- **Community & Boards**: 학교별 커뮤니티 및 주제별 게시판 (익명, 비밀글 지원)
- **Posts & Comments**: 다중 이미지, 좋아요, 신고, 계층형 댓글 기능
- **Real-time Chat**: WebSocket을 이용한 1:1 및 그룹 채팅
- **Friends & Block**: 친구 관계 관리 및 사용자 차단 기능
- **Timetable**: 학기별 시간표 생성, 공유 및 공개/비공개 설정
- **Notifications**: FCM을 통한 실시간 푸시 알림

## 3. Tech Stack & Architecture

### Tech Stack

- **Language**: Java 17
- **Framework**: Spring Boot 3.x, Spring Framework
- **Data Access**: Spring Data JPA, MyBatis
- **Database**: RDBMS (Enum 타입 사용)
- **Real-time**: WebSocket
- **Cache**: Redis
- **Build**: Gradle

### Architecture

본 프로젝트는 도메인 주도 설계(DDD)를 기반으로 한 계층형 아키텍처를 따릅니다.

```
com.app.nonstop
 ├── global       // 공통 설정 (Security, CORS, WebSocket, Redis) 및 유틸리티
 ├── infra        // 외부 시스템 연동 (AWS S3, FCM)
 └── domain       // 비즈니스 로직 (Auth, User, Chat, Community, Timetable 등)
```

- **`global`**: SecurityConfig, WebMvcConfig 등 애플리케이션 전역 설정과 공통 예외 처리, 응답 형식을 관리합니다.
- **`infra`**: AWS S3 이미지 업로더, FCM 푸시 알림 서비스 등 외부 인프라와의 통신을 담당합니다.
- **`domain`**: 각 도메인(Auth, User, Chat 등)별로 Controller, Service, DTO, Mapper를 구성하여 비즈니스 로직을 캡슐화합니다.

## 4. Database Schema

데이터베이스는 사용자, 인증, 커뮤니티, 채팅, 시간표 등 다양한 도메인별로 테이블이 구성되어 있습니다. 주요 테이블은 다음과 같습니다.

- `users`: 사용자 정보, 역할, 인증 상태 관리
- `universities`, `majors`: 대학 및 전공 정보
- `posts`, `comments`: 게시글 및 댓글 정보
- `chat_rooms`, `messages`: 채팅방 및 메시지 정보
- `time_tables`, `time_table_entries`: 시간표 및 수업 정보
- `friends`, `user_blocks`: 친구 및 차단 관계
- `notifications`, `reports`: 알림 및 신고 내역

자세한 내용은 `docs/erd.md` 파일을 참고하십시오.

## 5. API Endpoints

주요 API 엔드포인트는 다음과 같습니다. 전체 목록은 `docs/prd_draft.md` 문서를 확인하십시오.

| Method | URI                                    | Description                     |
|--------|----------------------------------------|---------------------------------|
| POST   | /api/v1/auth/signup                    | 이메일 회원가입                  |
| POST   | /api/v1/auth/login                     | 이메일 로그인                   |
| POST   | /api/v1/auth/google                    | Google 로그인                   |
| POST   | /api/v1/auth/refresh                   | Access Token 재발급             |
| GET    | /api/v1/users/me                       | 내 정보 조회                             |
| PATCH  | /api/v1/users/me                       | 프로필 수정                              |
| POST   | /api/v1/verification/student-id        | 학생증 사진 업로드 인증 요청       |
| GET    | /api/v1/boards/{boardId}/posts         | 게시글 목록           |
| POST   | /api/v1/boards/{boardId}/posts         | 게시글 작성           |
| GET    | /api/v1/posts/{postId}                 | 게시글 상세           |
| GET    | /api/v1/chat/rooms                     | 채팅방 목록                        |
| POST   | /api/v1/chat/rooms                     | 1:1 채팅방 생성                    |
| WS     | wss://api.nonstop.app/ws/v1/chat       | 실시간 채팅                        |
| GET    | /api/v1/timetables                     | 내 시간표 목록                  |
| POST   | /api/v1/timetables                     | 시간표 생성                     |

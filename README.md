# 🚀 Nonstop Backend

대학생 전용 **실명 기반 커뮤니티 모바일 앱 _Nonstop_**의 백엔드 REST API 서버입니다.  
본 문서는 프로젝트 구조, 핵심 기능, 실행 방법, 아키텍처를 한눈에 이해할 수 있도록 구성된 종합 가이드입니다.

---

## 📌 Overview

**Nonstop**는 대학생을 위한 커뮤니티 플랫폼으로,  
초기 진입 장벽을 낮추되 점진적인 인증을 통해 **신뢰도 높은 커뮤니티**를 구축하는 것을 목표로 합니다.

- 학교 인증 없이도 기본 기능 사용 가능
- 인증 완료 시 커뮤니티·게시판 등 확장 기능 제공
- 실명 기반 + 익명 선택을 병행한 균형 잡힌 커뮤니티 설계

---

## ✨ Core Features

### 🔐 Authentication & Authorization
- 이메일 / 비밀번호 로그인
- Google OAuth 2.0 로그인
- JWT 기반 인증
  - Access Token: **30분**
  - Refresh Token: **30일 (DB 저장)**

### 🏫 University Verification
- 대학 이메일 도메인(`@*.ac.kr`) 자동 인증
- 학생증 이미지 업로드 → 관리자 수동 검증

### 🧩 Graceful Degradation Policy
- 미인증 사용자도 다음 기능 사용 가능
  - 프로필 관리
  - 1:1 채팅
  - 시간표 생성
- 인증 사용자 전용 기능
  - 커뮤니티 게시판
  - 학교/전공 기반 콘텐츠

### 💬 Real-time Communication
- **Kafka 기반**의 확장 가능한 실시간 채팅 시스템 (Producer-Consumer 모델)
- WebSocket 기반
  - 1:1 채팅
  - 그룹 채팅

### 📝 Community
- 학교별 / 주제별 게시판
- 계층형 댓글 (댓글 · 대댓글)
- 좋아요 / 신고
- 익명글 / 비밀글 지원

### 📅 Timetable
- 학기별 시간표 관리
- 공개 설정 시 같은 학교 사용자에게 공유

### 🔔 Notifications
- Firebase Cloud Messaging (FCM)
- 친구 요청, 댓글, 채팅 메시지 등 실시간 알림

---

## 🛠 Tech Stack

### Backend
- **Java 17**
- **Spring Boot 3.4.x**
- Spring Security / OAuth2 Client
- JSON Web Token (jjwt)
- MyBatis

### Data
- PostgreSQL
- Redis

### Infra & External
- Azure Blob Storage (이미지 저장)
- Firebase Admin SDK (푸시 알림)
- Spring Mail (이메일 인증)

### Dev Tools
- Lombok
- Spring Boot DevTools
- SpringDoc OpenAPI (Swagger)

---

## ▶️ Getting Started (Local)

### Prerequisites
- Java 17
- Gradle 8.x
- PostgreSQL
- Redis

### Environment Configuration

프로젝트 루트에 `.env` 파일을 생성합니다. (env파일은 google drive에서 다운)


### Run

```bash
./gradlew bootRun
```

* 기본 포트: **8080**
* DevTools 적용 → 코드 변경 시 자동 재시작

---

## 🐳 Getting Started (Docker)

### Prerequisites

* Docker
* Docker Compose

### Run

```bash
docker-compose up --build
```

### Services

| Service | Description             |
| ------- | ----------------------- |
| app     | Spring Boot Application |
| db      | PostgreSQL              |
| redis   | Redis                   |

* `app` 컨테이너만 8080 포트 노출
* `db`, `redis`는 내부 네트워크 전용
* DB Health Check 이후 app 실행

### Dockerfile

* Multi-stage build

  1. **Build**: Gradle + JDK 17
  2. **Run**: OpenJDK 17 JRE (Slim)

---

## 📖 API Documentation

* **Swagger UI**
  👉 [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

* **OpenAPI Spec**
  👉 [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

---

## 📂 Project Structure

```
com.app.nonstop
 ├── global
 │   ├── config        // Security, Web, Redis, CORS
 │   ├── security
 │   ├── common        // 공통 응답, 예외
 │   └── util
 ├── infra
 │   ├── blob          // Azure Blob Storage
 │   └── fcm           // Firebase
 └── domain
     ├── auth
     ├── user
     ├── file          // 파일 업로드 (SAS URL 방식)
     ├── chat
     ├── community
     ├── notification
     └── timetable
```

* 도메인 단위로 Controller / Service / DTO / Mapper 구성
* DDD 기반 계층형 아키텍처

---

## 🗄 Database
> 전체 ERD는 `docs/erd.md` 참고

---

## 🧑‍💻 Coding Conventions

* Java 17 / Spring Boot 3.x 적극 활용
* RESTful API 설계
* Constructor Injection
* SOLID 원칙 준수
* SLF4J + Logback 로깅
* Swagger 기반 API 문서 자동화

> 상세 규칙: `docs/gemini.md`

---

## 📎 Related Docs

* `docs/prd_draft.md` – Product Requirements Document
* `docs/erd.md` – Database ERD
* `docs/gemini.md` – Coding Convention & Guidelines

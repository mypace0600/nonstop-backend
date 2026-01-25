# 🚀 Nonstop Backend

**버전:** v2.5.18 (Golden Master)
**진척도:** Backend Status 98% Completed
**최종 업데이트:** 2026-01-24

대학생 전용 **실명 기반 커뮤니티 모바일 앱 _Nonstop_**의 백엔드 REST API 서버입니다.
본 문서는 프로젝트 구조, 핵심 기능, 실행 방법, 아키텍처를 한눈에 이해할 수 있도록 구성된 종합 가이드입니다.

---

## 📌 Overview

**Nonstop**는 대학생을 위한 커뮤니티 플랫폼으로,
초기 진입 장벽을 낮추되 점진적인 인증을 통해 **신뢰도 높은 커뮤니티**를 구축하는 것을 목표로 합니다.

### 핵심 가치
- **Graceful Degradation:** 학교 인증 없이도 기본 기능(프로필, 채팅, 시간표 등) 사용 가능
- **University Verification:** 학교 이메일 및 학생증 인증을 통한 신뢰성 확보
- **Real-time Interaction:** Kafka 기반 대규모 채팅 및 실시간 알림 시스템

---

## ✨ Core Features & Status

### 🔐 Authentication & Authorization (✅ Fully Implemented)
- **JWT 기반 인증**: Access Token(30분), Refresh Token(30일, DB 저장 및 Rotation)
- **로그인 방식**: 이메일/비밀번호, Google OAuth 2.0
- **Auto Login**: Secure Storage 및 Interceptor 기반 자동 로그인/토큰 갱신 흐름 지원
- **회원가입 이메일 인증**: 6자리 인증 코드 발송/검증 (Redis TTL 5분)
- **정책 동의 시스템**: 필수 약관 동의 검증 (PolicyAgreementFilter)
- **로그인/로그아웃 이력 관리**: 보안 감사용 활동 기록
- **연령 제한**: 만 14세 미만 가입 제한 (생년월일 검증)

### 🏫 University Verification (✅ Fully Implemented)
- **학교 웹메일 인증**: 학교 도메인(`@*.ac.kr`) 자동 인식 및 6자리 인증 코드 발송/검증 (Redis TTL 활용)
- **학생증 인증**: 이미지 업로드(Azure Blob) 후 관리자 승인 프로세스 (업로드 로직 구현 완료)
- **검증 상태 관리**: `isVerified` 플래그를 통한 기능 차등 제공

### 💬 Real-time Chat (✅ Fully Implemented)
**Kafka + WebSocket(STOMP) 기반 확장 가능한 아키텍처**
- **1:1 & 그룹 채팅**: 방 생성, 초대, 강퇴, 메시지 전송/수신
- **Message Reliability**: `roomId`를 Kafka 키로 사용하여 순서 보장, `clientMessageId`로 멱등성 보장
- **Image Upload**: Azure Blob SAS URL 기반 직접 업로드 → 서버 메타데이터 저장 방식
- **읽음 처리**: `last_read_message_id` 및 `unread_count` 관리
- **기타**: 나에게만 삭제(카카오톡 스타일), 시스템 메시지(초대/퇴장) 지원

### 📅 Timetable (✅ Fully Implemented)
- **학기 관리**: 학기별 시간표 생성 (학기당 1개 제한)
- **수업 관리**: 요일/시간 중복 방지 로직, 색상 지정
- **공개/비공개**: 같은 학교 인증 사용자 간 시간표 공유 기능

### 📝 Community (✅ Fully Implemented)
- **게시판 구조**: 커뮤니티 > 게시판 > 게시글 > 댓글 > 대댓글
- **기능**: CRUD, 좋아요(Soft Delete), 신고, 익명/비밀글 지원
- **접근 제어**: 공통 커뮤니티(전체) vs 학교 커뮤니티(인증 사용자 전용)
- **편의성**: `isMine`, `writerId` 필드 반환으로 프론트엔드 작성자 판별 용이

### 👥 Friends, Block & Report (✅ Fully Implemented)
- **친구 관리**: 요청/수락/거절, 친구 목록 조회, 친구 삭제
- **사용자 차단**: 차단/해제, 차단 목록 조회, 채팅 및 친구 추가 제한
- **신고 시스템**: 게시글/댓글/사용자/채팅 메시지 신고 지원

### 🔔 Notifications (✅ Fully Implemented)
- **FCM (Firebase Cloud Messaging)**: 서버 트리거 기반 푸시 알림 전송
- **알림 센터**: 인앱 알림 목록 조회, 읽음 처리, 전체 읽음

### 📂 File Management (✅ Fully Implemented)
- **Azure Blob Storage**: SAS URL(Shared Access Signature) 발급을 통한 클라이언트 직접 업로드 방식
- **보안**: 용도(`purpose`)별 경로 분리 및 권한 제어

### 🛠 Admin Features (✅ Fully Implemented)
- **인증 관리**: 학생증 인증 요청 목록 조회, 승인/반려 처리
- **신고 관리**: 신고 목록 조회 (게시글/댓글/채팅/사용자), 콘텐츠 블라인드/삭제/기각 처리
- **사용자 관리**: 사용자 목록 조회, 권한(USER/ADMIN) 및 상태(활성/비활성) 변경
- **정책 관리**: 약관 CRUD, CDN 문서 업로드, 버전 관리

---

## 🛠 Tech Stack

### Backend
- **Java 17**
- **Spring Boot 3.4.12**
- Spring Security / OAuth2 Client
- **JWT (jjwt)**: 토큰 기반 인증
- **MyBatis**: SQL Mapper Framework

### Real-time & Messaging
- **Apache Kafka**: 메시지 브로커 (SASL_SSL, Idempotent Producer, Transactional)
- **WebSocket (STOMP)**: 실시간 양방향 통신
- **Redis**: 인증 코드(TTL), 세션 및 캐싱
- **Bucket4j**: Rate Limiting (WebSocket 60 msg/min)

### Data Storage
- **PostgreSQL**: 주 데이터베이스 (Soft Delete, Flyway)
- **Azure Blob Storage**: 이미지 및 파일 저장소

### Infra & Ops
- **Docker & Docker Compose**: 컨테이너 기반 배포 환경
- **Firebase Admin SDK**: 푸시 알림

---

## ▶️ Getting Started (Local)

### Prerequisites
- Java 17
- Gradle 8.x
- PostgreSQL
- Redis
- Apache Kafka (또는 Azure Event Hubs)

### Run Application
```bash
./gradlew bootRun
```
* 서버 포트: **28080**
* 프로필: `local` (기본값)

---

## 🐳 Getting Started (Docker)

```bash
docker-compose up --build
```
* `app`, `db`, `redis`, `zookeeper`, `kafka`, `init-kafka` 컨테이너가 실행됩니다.
* Kafka 토픽(`chat-messages`, `chat-read-events`)은 `init-kafka` 컨테이너에 의해 자동으로 생성됩니다.

---

## 📖 API Documentation

서버 실행 후 브라우저에서 아래 주소로 접속하여 API 명세를 확인할 수 있습니다.

- **Swagger UI**: [http://localhost:28080/swagger-ui.html](http://localhost:28080/swagger-ui.html)
- **OpenAPI Spec**: [http://localhost:28080/api-docs](http://localhost:28080/api-docs)

---

## 📂 Project Structure

```
com.app.nonstop
 ├── domain               // 도메인별 비즈니스 로직 (DDD)
 │   ├── admin            // 관리자 기능 (인증/신고/사용자 관리)
 │   ├── auth             // 인증 (Login, Signup, Token, Email Verification)
 │   ├── chat             // 채팅 (Kafka Producer/Consumer, WebSocket)
 │   ├── community        // 커뮤니티 (Board, Post, Comment)
 │   ├── device           // 기기 및 FCM 토큰
 │   ├── file             // 파일 업로드 (SAS URL)
 │   ├── friend           // 친구 관계 및 차단
 │   ├── notification     // 알림 서비스
 │   ├── policy           // 정책 및 약관 동의
 │   ├── report           // 신고 (게시글/댓글/사용자)
 │   ├── timetable        // 시간표 및 수업
 │   ├── university       // 대학 및 전공 정보
 │   ├── user             // 사용자 프로필
 │   └── verification     // 학교 이메일/학생증 인증
 ├── global
 │   ├── config           // 설정 (Security, Kafka, Redis, Web, etc.)
 │   ├── common           // 공통 모듈 (Response, Exception, Entity)
 │   ├── security         // Spring Security, JWT, PolicyAgreementFilter
 │   └── util             // 유틸리티
 ├── infra                // 인프라 연동 (Azure Blob)
 └── mapper               // MyBatis Mapper Interface
```

---

## 📞 Contact & Support
**개발팀**: Nonstop Backend Team
**문의**: 이슈 트래커 또는 메일
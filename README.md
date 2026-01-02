# 🚀 Nonstop Backend

**버전:** v2.2 (Golden Master)
**최종 업데이트:** 2025-12-29

대학생 전용 **실명 기반 커뮤니티 모바일 앱 _Nonstop_**의 백엔드 REST API 서버입니다.
본 문서는 프로젝트 구조, 핵심 기능, 실행 방법, 아키텍처를 한눈에 이해할 수 있도록 구성된 종합 가이드입니다.

---

## 📌 Overview

**Nonstop**는 대학생을 위한 커뮤니티 플랫폼으로,
초기 진입 장벽을 낮추되 점진적인 인증을 통해 **신뢰도 높은 커뮤니티**를 구축하는 것을 목표로 합니다.

### 핵심 가치
- 학교 인증 없이도 기본 기능 사용 가능 (Graceful Degradation)
- 인증 완료 시 커뮤니티·게시판 등 확장 기능 제공
- 실명 기반 + 익명 선택을 병행한 균형 잡힌 커뮤니티 설계
- Kafka 기반 확장 가능한 실시간 채팅 시스템

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
**Kafka + WebSocket 기반 확장 가능한 실시간 채팅 시스템**

#### 아키텍처
```
Client → WebSocket (STOMP) → Producer → Kafka Topic
  → Consumer → DB 저장 + WebSocket 브로드캐스팅 → Client
```

#### 주요 기능
- 1:1 채팅 (중복 방지 자동 처리)
- 그룹 채팅 (초대/강퇴 기능)
- 메시지 순서 보장 (roomId를 Kafka 메시지 키로 사용)
- 멱등성 보장 (clientMessageId 기반 중복 방지)
- 읽음 처리 (last_read_message_id 관리)
- 카카오톡 스타일 메시지 삭제 (나에게만 삭제)

#### 기술 스택
- **WebSocket/STOMP**: 실시간 양방향 통신
- **Apache Kafka**: 메시지 브로커 (확장성 및 안정성)
- **Redis**: 세션 관리
- **PostgreSQL**: 메시지 영구 저장

> 상세 설계: `docs/kafka-websocket-chat-review.md`, `docs/prd_draft.md` (섹션 3.7)

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

### Real-time & Messaging
- **Apache Kafka** (메시지 브로커)
  - SASL_SSL 보안 프로토콜
  - 멱등성 프로듀서 (enable.idempotence)
  - 트랜잭셔널 메시징 지원
- **WebSocket/STOMP** (실시간 양방향 통신)
- **SockJS** (WebSocket fallback)

### Data
- **PostgreSQL** (주 데이터베이스)
  - ENUM 타입 적극 활용
  - Soft Delete 기반 설계
  - Flyway 마이그레이션
- **Redis** (캐시 및 세션 관리)

### Infra & External
- **Azure Blob Storage** (이미지 직접 업로드 with SAS URL)
- **Firebase Cloud Messaging** (푸시 알림)
- **Spring Mail** (이메일 인증)

### Dev Tools
- Lombok
- Spring Boot DevTools
- SpringDoc OpenAPI (Swagger)
- HikariCP (커넥션 풀)

### Build & Deploy
- Gradle 8.x
- Docker & Docker Compose
- Multi-stage Docker build

---

## ▶️ Getting Started (Local)

### Prerequisites
- Java 17
- Gradle 8.x
- PostgreSQL
- Redis
- **Apache Kafka** (또는 Azure Event Hubs for Kafka)

### Environment Configuration

프로젝트 루트에 `.env` 파일을 생성합니다. (env파일은 google drive에서 다운)

#### 필수 환경변수
```bash
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=nonstop
DB_USERNAME=postgres
DB_PASSWORD=

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Kafka
KAFKA_BOOTSTRAP_SERVERS=
KAFKA_CONNECTION_STRING=

# Azure Blob Storage
AZURE_STORAGE_ACCOUNT_NAME=
AZURE_STORAGE_ACCOUNT_KEY=

# Auth
JWT_SECRET_KEY=
GOOGLE_CLIENT_ID=

# FCM
FIREBASE_CREDENTIALS_PATH=
```

> 전체 환경변수 목록: `docs/production-checklist.md` (섹션 6)

### Run

```bash
./gradlew bootRun
```

* 기본 포트: **28080**
* DevTools 적용 → 코드 변경 시 자동 재시작
* 프로필: `local` (기본값)

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

### Swagger UI
👉 [http://localhost:28080/swagger-ui.html](http://localhost:28080/swagger-ui.html)

### OpenAPI Spec
👉 [http://localhost:28080/api-docs](http://localhost:28080/api-docs)

### API 엔드포인트 요약
전체 API 엔드포인트 목록은 `docs/prd_draft.md` (섹션 4) 참고

#### 주요 엔드포인트
- **인증**: `/api/v1/auth/*` (회원가입, 로그인, 토큰 갱신)
- **사용자**: `/api/v1/users/*` (프로필, 인증 상태)
- **채팅**: `/api/v1/chat/*` (채팅방, 메시지)
  - WebSocket: `wss://api.nonstop.app/ws/v1/chat`
- **커뮤니티**: `/api/v1/communities/*`, `/api/v1/boards/*`
- **게시글**: `/api/v1/posts/*`
- **친구**: `/api/v1/friends/*`
- **시간표**: `/api/v1/timetables/*`
- **알림**: `/api/v1/notifications/*`
- **파일**: `/api/v1/files/*` (SAS URL 발급, 업로드 완료)

---

## 📂 Project Structure

```
com.app.nonstop
 ├── global
 │   ├── config           // Security, Kafka, WebSocket, Azure Blob, Firebase
 │   │   ├── SecurityConfig.java
 │   │   ├── KafkaProducerConfig.java
 │   │   ├── KafkaConsumerConfig.java
 │   │   ├── WebSocketConfig.java
 │   │   ├── AzureBlobStorageConfig.java
 │   │   └── FirebaseConfig.java
 │   ├── security
 │   │   ├── jwt          // JWT 토큰 발급/검증
 │   │   ├── oauth2       // Google OAuth2
 │   │   └── user         // CustomUserDetails
 │   ├── common
 │   │   ├── entity       // BaseTimeEntity
 │   │   ├── response     // ApiResponse
 │   │   └── exception    // 공통 예외
 │   └── util
 ├── infra
 │   └── blob             // Azure Blob Storage Uploader
 ├── mapper                // MyBatis Mapper 인터페이스 (XML 매핑)
 │   ├── AuthMapper.java
 │   ├── ChatMapper.java
 │   ├── UserMapper.java
 │   └── ...
 └── domain
     ├── auth             // 인증 (회원가입, 로그인, 토큰)
     ├── user             // 사용자 관리
     ├── verification     // 대학생 인증
     ├── device           // FCM 토큰 관리
     ├── friend           // 친구 관계 및 차단
     ├── chat             // 실시간 채팅 (Kafka + WebSocket)
     │   ├── controller
     │   ├── service
     │   │   ├── ChatKafkaProducer.java
     │   │   └── ChatKafkaConsumer.java
     │   ├── dto
     │   └── entity
     ├── community        // 커뮤니티 및 게시판
     ├── file             // 파일 업로드 (SAS URL 방식)
     ├── notification     // 알림
     └── timetable        // 시간표
```

### 아키텍처 특징
- **도메인 단위 계층 구조**: Controller → Service → Mapper (MyBatis)
- **DDD 기반 설계**: 각 도메인은 독립적인 Entity, DTO, Exception 보유
- **MyBatis XML 매핑**: `resources/mybatis/mappers/` 디렉토리에 SQL 쿼리 분리
- **설정 분리**: global/config에서 모든 외부 서비스 및 프레임워크 설정 관리

> 상세 패키지 구조: `docs/package_frame.md`

---

## 🗄 Database

### Schema
**DBMS**: PostgreSQL
**버전**: Golden Master v2.2

#### 주요 테이블
- **users**: 사용자 정보 (인증, 프로필)
- **universities, majors**: 대학 및 전공 정보
- **communities, boards, posts, comments**: 커뮤니티 계층 구조
- **chat_rooms, messages**: 채팅 (1:1 및 그룹)
- **friends, user_blocks**: 친구 관계 및 차단
- **time_tables**: 시간표
- **notifications**: 알림

#### 특징
- **ENUM 타입 적극 활용**: 상태 관리의 타입 안정성 보장
- **Soft Delete**: `deleted_at` 필드로 논리 삭제 구현
- **방향 없는 관계**: `LEAST/GREATEST` 기반 Unique Index (친구, 1:1 채팅방)
- **Flyway 마이그레이션**: 버전 관리 및 자동 스키마 업데이트

> 전체 ERD: `docs/erd.md`
> DDL 스크립트: `docs/DDL.md`, `docs/251226-ddl.sql`

---

## 🧑‍💻 Coding Conventions

### 핵심 원칙
- Java 17 / Spring Boot 3.x 최신 기능 활용
- RESTful API 설계 (적절한 HTTP 메서드 및 상태 코드)
- Constructor Injection (필드 주입 지양)
- SOLID 원칙 준수
- SLF4J + Logback 로깅

### 네이밍 컨벤션
- **클래스명**: PascalCase (예: `UserController`, `ChatService`)
- **메서드/변수명**: camelCase (예: `findUserById`, `isVerified`)
- **상수**: ALL_CAPS (예: `MAX_RETRY_ATTEMPTS`)

### 코드 품질
- MyBatis 기반 데이터 접근 (XML 매핑)
- Bean Validation (`@Valid`) 적극 활용
- Exception Handling: `@ControllerAdvice` + `@ExceptionHandler`
- Swagger/OpenAPI 기반 API 문서 자동화

> 상세 가이드: `docs/gemini.md`

---

## 🚀 Production Checklist

### MVP 출시 전 필수 항목
- [ ] Graceful Shutdown 설정
- [ ] Kafka DLQ (Dead Letter Queue) 구현
- [ ] WebSocket JWT 인증 추가
- [ ] Redis 패스워드 설정 (prod 프로필)
- [ ] Kafka Consumer Concurrency 설정
- [ ] 구조화 로깅 (JSON) 적용

### 모니터링 및 운영
- [ ] Health Check 엔드포인트 구성 (Actuator)
- [ ] 에러 알림 (Slack/Email) 연동
- [ ] 분산 추적 (Micrometer Tracing) 설정
- [ ] Kafka Consumer Lag 모니터링

### 보안
- [ ] Kafka SASL/SSL 활성화
- [ ] WebSocket 세션 제한
- [ ] Rate Limiting 적용 (Bucket4j)

> 상세 체크리스트: `docs/production-checklist.md`

---

## 📊 System Review

### Kafka & WebSocket 채팅 시스템
**종합 점수**: 74/100

#### 현재 상태
- ✅ Kafka 기반 확장 가능한 아키텍처
- ✅ 메시지 순서 보장 (roomId 키 사용)
- ✅ 기본적인 멱등성 설정
- ⚠️ WebSocket 인증 미구현 (CRITICAL)
- ⚠️ DLQ, Graceful Shutdown 없음

#### MVP 출시 가능 여부
**현재**: ❌ 불가 (보안 이슈)
**Phase 1 완료 후**: ✅ 가능 (2-3일 소요)

> 상세 검토 리포트: `docs/kafka-websocket-chat-review.md`

---

## 📎 Related Docs

### 제품 및 기획
- `docs/prd_draft.md` – Product Requirements Document (v2.1)

### 데이터베이스
- `docs/erd.md` – Entity Relationship Diagram
- `docs/DDL.md` – 데이터베이스 스키마 정의
- `docs/251226-ddl.sql` – 실제 DDL 스크립트

### 아키텍처 및 설계
- `docs/package_frame.md` – 패키지 구조
- `docs/kafka-websocket-chat-review.md` – 채팅 시스템 검토 리포트

### 운영 및 배포
- `docs/production-checklist.md` – 프로덕션 배포 체크리스트

### 개발 가이드
- `docs/gemini.md` – Java/Spring Boot 코딩 컨벤션

---

## 📞 Contact & Support

**프로젝트 문의**: 개발팀
**버전**: v2.2 (Golden Master)
**최종 업데이트**: 2025-12-29

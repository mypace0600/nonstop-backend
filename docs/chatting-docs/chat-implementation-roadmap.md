# 채팅 시스템 구현 로드맵 (Chat System Implementation Roadmap)

**버전:** v1.0
**작성일:** 2026-01-08
**상태:** Draft

---

## 1. 개요 (Overview)
본 문서는 현재 채팅 시스템의 구현 상태와 설계 문서(PRD, Design Docs) 간의 격차를 해소하기 위한 단계별 실행 계획을 정의합니다.
주요 목표는 **읽음 처리의 비동기화**, **WebSocket 안정성 확보**, 그리고 **Kafka 운영 환경 최적화**입니다.

## 2. 구현 마일스톤 (Milestones)

### ✅ Milestone 1: Kafka 인프라 및 설정 고도화 (Infrastructure)
**목표:** 운영 환경(Azure Event Hubs)과 로컬 환경(Docker)을 구분하고, 토픽의 내구성 및 안정성을 확보합니다.

- [x] **환경별 Kafka 설정 분리**
    - `KafkaTopicConfig.java`를 수정하여 `@Profile("local")`과 `@Profile("prod")`로 빈 생성을 분리합니다.
    - `prod` 프로필에서는 자동 토픽 생성을 비활성화(`allow.auto.create.topics: false`)하고 Azure Event Hubs 제약 사항을 반영합니다.
- [x] **토픽 상세 설정 적용**
    - `KafkaTopicConfig`에 토픽별 보존 기간(Retention), 파티션 수, Replica 설정을 명시합니다.
    - 상수 클래스(`KafkaTopicConfig.Topics`)를 도입하여 하드코딩된 문자열을 제거합니다.
- [x] **DLQ (Dead Letter Queue) 토픽 정의**
    - `chat-messages-dlt`, `chat-read-events-dlt` 등 실패 처리를 위한 토픽을 정의합니다.

### ✅ Milestone 2: 읽음 처리 비동기화 (Asynchronous Read Receipts)
**목표:** 동기식 DB 업데이트로 인한 부하를 제거하고, 실시간 읽음 상태 동기화를 구현합니다.

- [ ] **DTO 및 이벤트 정의**
    - `ChatReadEventDto` (Kafka 페이로드) 및 `ChatReadStatusDto` (WebSocket 브로드캐스트용)를 생성합니다.
- [ ] **Producer 구현 (`ChatReadEventProducer`)**
    - `chat-read-events` 토픽으로 이벤트를 발행하는 서비스 로직을 구현합니다.
    - Kafka Key를 `userId`로 설정하여 순서를 보장합니다.
- [ ] **Consumer 구현 (`ChatReadEventConsumer`)**
    - 이벤트를 소비하여 DB (`chat_room_members.last_read_message_id`)를 업데이트합니다.
    - WebSocket (`/sub/chat/room/{roomId}/read`)을 통해 실시간으로 참여자들에게 읽음 상태를 전송합니다.
- [ ] **Controller 리팩토링**
    - `ChatController.markAsRead` 메서드가 DB를 직접 호출하지 않고 Producer를 호출하도록 수정합니다.
- [ ] **Unread Count 로직 (MVP)**
    - 실시간 쿼리 기반의 `countUnreadMessages` 메서드를 Mapper에 추가합니다.

### ✅ Milestone 3: WebSocket 안정성 및 보안 (Stability & Security)
**목표:** 리소스 고갈 공격(DoS)을 방지하고 세션 관리를 강화합니다.

- [ ] **설정 중앙화 (`WebSocketProperties`)**
    - 세션 수, 메시지 크기, 타임아웃 등 하드코딩된 설정값을 `application.yml` 및 Properties 클래스로 추출합니다.
- [ ] **세션 제한 구현 (`WebSocketSessionManager`)**
    - Redis를 활용하여 사용자당 최대 세션 수(예: 3개)를 제한합니다.
    - 초과 연결 시 가장 오래된 세션을 종료하는 LIFO 로직을 구현합니다.
- [ ] **메시지 크기 및 속도 제한**
    - `WebSocketConfig`에서 메시지 크기(64KB) 및 버퍼 사이즈를 제한합니다.
    - `WebSocketRateLimitInterceptor`를 구현하여 사용자당 전송 속도(예: 60 msg/min)를 제어합니다.

### ✅ Milestone 4: 모니터링 및 운영 준비 (Operation Readiness)
**목표:** 장애 감지 및 복구 체계를 구축합니다.

- [ ] **Kafka 에러 핸들링**
    - Consumer 레벨에서 재시도(Retry) 로직 및 DLT 연동을 구현합니다.
- [ ] **로깅 표준화**
    - Kafka 메시지 발행/수신, WebSocket 연결/종료 시 주요 메타데이터를 포함한 구조화된 로그를 남깁니다.

---

## 3. 우선순위 및 일정 (Priorities)

| 순위 | 작업 항목 | 예상 공수 | 비고 |
|:---:|---|:---:|---|
| **1** | **Milestone 1 (Kafka 설정)** | 1일 | 배포 안정성 필수 |
| **2** | **Milestone 2 (읽음 처리)** | 2일 | DB 부하 감소 핵심 |
| **3** | **Milestone 3 (WebSocket)** | 2일 | 보안 및 안정성 |
| **4** | **Milestone 4 (모니터링)** | 1일 | 운영 편의성 |

## 4. 참고 문서
- `docs/chatting-docs/kafka-topic-config-design.md`
- `docs/chatting-docs/chat-read-events-design.md`
- `docs/chatting-docs/websocket-session-limit-design.md`

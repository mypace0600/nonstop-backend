# Nonstop 채팅 시스템 통합 설계서 (Chat System Overview)

**버전:** v1.0
**최종 수정일:** 2026-01-08

---

## 1. 시스템 개요 (System Overview)

Nonstop의 채팅 시스템은 **대규모 트래픽 처리가 가능한 실시간 메시징 시스템**을 목표로 합니다.
**WebSocket(STOMP)**을 통해 클라이언트와 실시간으로 통신하며, **Kafka**를 메시지 브로커로 사용하여 시스템의 결합도를 낮추고 데이터 유실을 방지합니다.

### 1.1 핵심 아키텍처 다이어그램

```mermaid
graph TD
    Client[Client (App/Web)] -->|WebSocket/STOMP| LB[Load Balancer]
    LB -->|Connection| WS_Server[API Server (Spring Boot)]
    
    subgraph "Messaging Layer"
        WS_Server -->|Produce| Kafka_P[Kafka Producer]
        Kafka_P -->|Topic: chat-messages| Topic_Msg[(Kafka Topic: Msg)]
        Kafka_P -->|Topic: chat-read-events| Topic_Read[(Kafka Topic: Read)]
    end
    
    subgraph "Processing Layer"
        Topic_Msg -->|Consume| Consumer_Msg[Chat Consumer]
        Topic_Read -->|Consume| Consumer_Read[Read Event Consumer]
    end
    
    subgraph "Storage Layer"
        Consumer_Msg -->|Insert| DB[(MySQL)]
        Consumer_Read -->|Update| DB
        Consumer_Read -->|Update| Redis[(Redis: Session/Cache)]
    end
    
    subgraph "Real-time Feedback"
        Consumer_Msg -->|Broadcast| WS_Server
        Consumer_Read -->|Broadcast| WS_Server
    end
```

---

## 2. 상세 설계 문서 참조 (Detailed Designs)

본 문서는 상위 레벨의 통합 문서이며, 각 컴포넌트의 상세 구현 명세는 아래 문서들을 참조합니다.

| 영역 | 문서명 | 주요 내용 |
|------|--------|-----------|
| **Kafka 설정** | [Kafka Topic & Infra Design](./kafka-topic-config-design.md) | 토픽 파티셔닝, Retention, Azure/Local 환경 분리, DLT 설정 |
| **읽음 처리** | [Read Receipts Design](./chat-read-events-design.md) | 읽음 이벤트 비동기 처리, Unread Count 관리, 브로드캐스팅 흐름 |
| **세션 관리** | [WebSocket Session Design](./websocket-session-limit-design.md) | 동시 접속 제한(3기기), Rate Limiting, 메시지 크기 제한 |
| **구현 계획** | [Implementation Roadmap](./chat-implementation-roadmap.md) | 단계별 개발 일정 및 우선순위 (Milestones) |

---

## 3. 데이터 흐름 (Data Flow)

### 3.1 채팅 메시지 전송 (Message Sending)
1. **Client**: `/pub/chat/message`로 메시지 전송 (WebSocket)
2. **Server**: `WebSocketChatController`가 수신 → 유효성 검증
3. **Kafka**: `ChatKafkaProducer`가 `chat-messages` 토픽으로 발행 (Key: roomId)
4. **Consumer**: `ChatKafkaConsumer`가 메시지 수신
5. **Persistence**: DB `message` 테이블에 저장
6. **Broadcast**: `/sub/chat/room/{roomId}` 구독자들에게 메시지 전송

### 3.2 읽음 처리 (Read Receipts) - *구현 예정*
1. **Client**: 채팅방 입장 시 API 호출 또는 소켓 이벤트 전송
2. **Server**: `ChatReadEventProducer`가 `chat-read-events` 토픽으로 발행 (Key: userId)
3. **Consumer**: `ChatReadEventConsumer`가 수신
4. **Persistence**: DB `chat_room_members.last_read_message_id` 업데이트
5. **Broadcast**: `/sub/chat/room/{roomId}/read`로 읽음 상태 전파 (상대방의 '1' 표시 제거)

---

## 4. 인프라 및 기술 스택 (Infrastructure)

### 4.1 Message Broker: Apache Kafka
- **운영 환경 (Prod):** Azure Event Hubs (Kafka Protocol 호환)
- **로컬 환경 (Local):** Docker Container
- **신뢰성:** `acks=all`, `idempotence=true` 설정으로 메시지 유실 및 중복 방지

### 4.2 Database: MySQL & Redis
- **MySQL:** 채팅방, 메시지, 멤버 정보 영구 저장
- **Redis:**
    - WebSocket 세션 관리 (중복 로그인 방지)
    - Rate Limiting (도배 방지)
    - (Optional) 최근 메시지 캐싱

---

## 5. 보안 및 안정성 정책 (Security & Stability)

### 5.1 인증 (Authentication)
- **Handshake:** `WebSocketAuthInterceptor`에서 JWT 토큰 검증
- **Sub/Pub 권한:** `StompHandler`에서 채팅방 참여자인지 검증

### 5.2 리소스 제한 (Resource Limits)
- **세션:** 유저당 최대 3개 디바이스 동시 접속 허용
- **메시지:** 최대 64KB
- **속도:** 분당 60개 메시지로 제한 (Spam Protection)

---

## 6. 디렉토리 구조 (Directory Structure)

```
docs/chatting-docs/
├── chat-system-overview.md         # [Main] 통합 설계서 (본 문서)
├── chat-implementation-roadmap.md  # [Plan] 구현 로드맵
├── kafka-topic-config-design.md    # [Detail] Kafka 상세 설계
├── chat-read-events-design.md      # [Detail] 읽음 처리 상세 설계
├── websocket-session-limit-design.md # [Detail] 세션 제한 상세 설계
└── (Archive)                       # 기존 리뷰 문서들은 참고용으로 보관
```

# Kafka & WebSocket 채팅 시스템 리뷰

**Version:** v3.1
**Last Updated:** 2026-01-04
**Status:** MVP 출시 가능

---

## 1. 요약

### 1.1 한 줄 결론

> **Kafka + WebSocket 기반 실시간 채팅 시스템은 핵심 보안/무결성 이슈를 모두 해결했으며, 현재 상태로 MVP 출시가 가능하다.**

### 1.2 종합 점수

| 항목 | 점수 | 평가 |
|------|------|------|
| 아키텍처 | 90/100 | Kafka 기반 확장 구조 |
| 보안 | 90/100 | JWT + senderId 강제 |
| 안정성 | 85/100 | DLQ, Graceful Shutdown |
| 확장성 | 90/100 | roomId 파티셔닝 |
| 운영 준비도 | 75/100 | 로깅/모니터링 보완 필요 |

**총점: 86/100**

---

## 2. 아키텍처

### 2.1 메시지 흐름

```
Client
  → WebSocket (STOMP)
  → WebSocketChatController
  → ChatKafkaProducer (roomId = key)
  → Kafka Topic (chat-messages)
  → ChatKafkaConsumer
  → ChatService (DB 저장)
  → WebSocket Broadcast
  → Subscribers
```

### 2.2 핵심 설계 포인트

- **roomId를 Kafka key로 사용** → 동일 채팅방 메시지 순서 보장
- **Kafka 중간 계층** → 서버 수평 확장 가능
- **DB 저장 후 브로드캐스트** → 데이터 정합성 유지

### 2.3 주요 컴포넌트

| 컴포넌트 | 파일 | 역할 |
|----------|------|------|
| WebSocket 엔드포인트 | `WebSocketConfig.java` | STOMP 설정 |
| 메시지 수신 | `WebSocketChatController.java` | 클라이언트 메시지 처리 |
| Kafka 발행 | `ChatKafkaProducer.java` | 메시지 Kafka 전송 |
| Kafka 소비 | `ChatKafkaConsumer.java` | 메시지 처리 및 저장 |
| 비즈니스 로직 | `ChatServiceImpl.java` | 검증, 저장, 브로드캐스트 |

---

## 3. 보안 및 무결성 상태

### 3.1 구현 완료 (CRITICAL)

| 항목 | 상태 | 구현 위치 |
|------|------|----------|
| WebSocket JWT 인증 | ✅ | `WebSocketAuthInterceptor.java` |
| senderId 위조 방지 | ✅ | `WebSocketChatController.java:34-35` |
| 채팅방 멤버 권한 검증 | ✅ | `ChatServiceImpl.java:38` |
| 메시지 유효성 검증 | ✅ | `ChatServiceImpl.java:62-90` |
| 메시지 중복 방지 | ✅ | `ChatServiceImpl.java:45-48` |

### 3.2 상세 검증 내역

**senderId 위조 방지:**
```java
// WebSocketChatController.java:34-35
Long authenticatedUserId = (Long) sessionAttributes.get("userId");
message.setSenderId(authenticatedUserId);  // 클라이언트 값 무시
```

**멤버 권한 검증:**
```java
// ChatServiceImpl.java:38
if (!chatRoomMapper.isMemberOfRoom(message.getRoomId(), message.getSenderId())) {
    log.warn("Unauthorized message attempt...");
    return;
}
```

**메시지 유효성 검증:**
- roomId 필수
- senderId 필수
- content 필수 (시스템 메시지 제외)
- 최대 5000자 제한

---

## 4. Kafka 설정 현황

### 4.1 현재 구현 상태

| 설정 | 상태 | 위치 |
|------|------|------|
| `enable.idempotence: true` | ✅ | `application.yml:76` |
| `acks: all` | ✅ | `application.yml:74` |
| `isolation.level: read_committed` | ✅ | `application.yml:87` |
| SASL_SSL 보안 | ✅ | `application.yml:63-68` |
| DLQ (chat-messages-dlt) | ✅ | `KafkaConsumerConfig.java` |
| Consumer concurrency: 3 | ✅ | `application.yml:90` |

### 4.2 토픽 설정 (KafkaTopicConfig.java)

| 토픽 | 파티션 | 레플리카 | 상태 |
|------|--------|----------|------|
| `chat-messages` | 10 | 3 | ✅ 구현됨 |
| `chat-messages-dlt` | 3 | 3 | ✅ 구현됨 |
| `chat-read-events` | 5 | 3 | ✅ 구현됨 |
| `chat-read-events-dlt` | - | - | ❌ 미구현 |

### 4.3 개선 필요 사항

설계 문서 (`docs/design/kafka-topic-config-design.md`) 대비 미구현 항목:

| 항목 | 현재 상태 | 설계 문서 권장 |
|------|----------|---------------|
| 환경별 Profile 분리 | ❌ 없음 | `@Profile("local")`, `@Profile("prod")` |
| Retention 설정 | ❌ 없음 | 토픽별 retention.ms 설정 |
| 토픽명 상수 클래스 | ❌ 없음 | `Topics` inner class |
| prod 자동 생성 비활성화 | ❌ 없음 | `allow.auto.create.topics: false` |

---

## 5. WebSocket 설정 현황

### 5.1 현재 구현 상태 (WebSocketConfig.java)

| 설정 | 상태 |
|------|------|
| STOMP 엔드포인트 (`/ws/v1/chat`) | ✅ |
| JWT 인증 인터셉터 | ✅ |
| SockJS fallback | ✅ |
| 메시지 브로커 (`/sub`, `/pub`) | ✅ |

### 5.2 개선 필요 사항

설계 문서 (`docs/design/websocket-session-limit-design.md`) 대비 미구현 항목:

| 항목 | 현재 상태 | 설계 문서 권장 |
|------|----------|---------------|
| 사용자당 세션 제한 | ❌ 없음 | 최대 3개 |
| 메시지 크기 제한 | ❌ 없음 | 64KB |
| 버퍼 크기 제한 | ❌ 없음 | 512KB |
| 하트비트 설정 | ❌ 없음 | 25초 간격 |
| Rate Limiting | ❌ 없음 | 60 msg/min |

**미구현 컴포넌트:**
- `WebSocketProperties.java`
- `WebSocketSessionManager.java`
- `WebSocketRateLimitInterceptor.java`

---

## 6. 기능 구현 상태

### 6.1 완전 구현

- 실시간 메시지 송수신 (Kafka 기반)
- 1:1 채팅 (중복 방지 포함)
- 그룹 채팅
- 메시지 저장 / 조회 / 삭제
- 그룹 채팅 이벤트 (INVITE / LEAVE / KICK)
- WebSocket 인증 및 권한 검증
- DLQ, Graceful Shutdown

### 6.2 미구현 (비필수 / 후순위)

- 읽음 처리 이벤트 (`chat-read-events`)
- Kafka 토픽 환경별 설정
- JSON 구조화 로깅
- WebSocket 세션 제한

### 6.3 PRD 대비 구현률

| 구분 | 완료 | 전체 | 비율 |
|------|------|------|------|
| 핵심 기능 | 12 | 14 | 85.7% |
| 전체 기능 | 13 | 16 | 81.3% |

---

## 7. 운영 설정 상태

### 7.1 구현 완료

| 항목 | 상태 | 위치 |
|------|------|------|
| Graceful Shutdown | ✅ | `application.yml:5-10` |
| Shutdown timeout 30s | ✅ | `application.yml:10` |
| Consumer concurrency | ✅ | `application.yml:90` |
| Actuator health endpoint | ✅ | `application.yml:154-161` |

### 7.2 미구현

| 항목 | 우선순위 |
|------|----------|
| JSON 구조화 로깅 | MEDIUM |
| 에러 알림 (Slack/Email) | MEDIUM |
| 분산 추적 (Zipkin) | LOW |

---

## 8. 액션 플랜

### Phase 1: 완료됨 (2026-01-04)

- [x] WebSocket JWT 인증
- [x] senderId 검증 (어뷰징 방지)
- [x] 멤버 권한 검증
- [x] 메시지 유효성 검증
- [x] clientMessageId 중복 방지

### Phase 2: 선택 사항 (운영 품질 향상)

| 작업 | 우선순위 |
|------|----------|
| Kafka 토픽 환경별 분리 | HIGH |
| 토픽명 상수 클래스 적용 | HIGH |
| JSON 구조화 로깅 | MEDIUM |
| CORS 도메인 제한 | MEDIUM |

### Phase 3: 정식 서비스 전

| 작업 | 우선순위 |
|------|----------|
| 읽음 처리 (`chat-read-events`) | MEDIUM |
| WebSocket 세션 제한 | MEDIUM |
| ChatController TODO API 구현 | MEDIUM |

### Phase 4: 대규모 트래픽 대비 (장기)

| 작업 | 우선순위 |
|------|----------|
| 서킷 브레이커 (Resilience4j) | LOW |
| 분산 추적 (Micrometer) | LOW |
| Redis 캐시 적용 | LOW |

---

## 9. 관련 설계 문서

| 문서 | 설명 | 상태 |
|------|------|------|
| `docs/design/kafka-topic-config-design.md` | Kafka 토픽 설정 상세 설계 | 구현 대기 |
| `docs/design/websocket-session-limit-design.md` | WebSocket 세션 제한 설계 | 구현 대기 |
| `docs/design/chat-read-events-design.md` | 읽음 처리 이벤트 설계 | 구현 대기 |

---

## 10. 결론

### 현재 상태 평가

**강점:**
- Kafka 기반의 확장 가능한 아키텍처
- 메시지 순서 보장 (roomId를 key로 사용)
- 멱등성 설정 + clientMessageId 중복 체크
- 1:1 및 그룹 채팅 완전 구현
- 모든 CRITICAL 보안 이슈 해결

**개선 필요:**
- Kafka 토픽 환경별 분리
- WebSocket 세션/Rate 제한
- 구조화 로깅 및 모니터링

### MVP 출시 가능 여부

**현재:** ✅ **출시 가능**

모든 CRITICAL 보안 이슈가 해결되었으며, 핵심 채팅 기능이 완전히 구현되었습니다.

### 1:1 및 그룹 채팅 지원 여부

✅ **완전히 구현됨**

| 기능 | 1:1 채팅 | 그룹 채팅 |
|------|----------|----------|
| 채팅방 생성 | ✅ | ✅ |
| 실시간 메시지 | ✅ | ✅ |
| 메시지 조회/삭제 | ✅ | ✅ |
| 초대/강퇴/이벤트 | - | ✅ |

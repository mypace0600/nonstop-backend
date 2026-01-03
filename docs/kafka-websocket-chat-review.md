# Kafka & WebSocket 기반 실시간 채팅 시스템 종합 검토 리포트

**작성일:** 2025-12-29
**버전:** v2.0
**최종 업데이트:** 2026-01-03
**검토 범위:** Kafka 설정, WebSocket 구현, 채팅 기능 (1:1 및 그룹)

---

## 📊 전체 요약

**Kafka와 WebSocket 기반의 실시간 채팅 시스템은 전반적으로 우수한 아키텍처**를 가지고 있습니다. PRD 문서의 핵심 설계 원칙을 잘 따르고 있으며, 메시지 순서 보장, 멱등성, WebSocket 인증, DLQ 등 핵심 기능이 구현되었습니다.

### 종합 점수

| 항목 | 점수 | 평가 |
|------|------|------|
| **아키텍처 설계** | 90/100 | Kafka 기반 설계 우수, 핵심 기능 구현 완료 |
| **보안** | 75/100 | WebSocket 인증 구현됨, senderId 검증 보완 필요 |
| **안정성** | 85/100 | DLQ, Graceful Shutdown, 중복 방지 구현됨 |
| **확장성** | 90/100 | Kafka 기반으로 우수한 확장성 |
| **운영 준비도** | 70/100 | 모니터링, 로깅 개선 필요 |

**총점: 82/100** (이전 74점에서 +8점 향상)

### MVP 출시 가능 여부

**현재 상태:** ✅ 조건부 출시 가능
**남은 작업:** senderId 검증, 트랜잭셔널 Producer 설정 후 출시 권장

---

## ✅ 현재 구현 상태 - 잘 된 부분

### 1. Kafka 핵심 설정 (application.yml:54-84)

```yaml
✅ SASL_SSL 보안 프로토콜
✅ enable.idempotence: true (멱등성)
✅ isolation.level: read_committed
✅ acks: all (신뢰성)
✅ trusted.packages 설정
✅ retries: 3
```

**평가:** Kafka 프로듀서와 컨슈머의 기본 설정이 PRD 요구사항을 충족합니다.

### 2. WebSocket STOMP 설정 (WebSocketConfig.java)

```
✅ /pub/chat/message (발행 엔드포인트)
✅ /sub/chat/room/{roomId} (구독 엔드포인트)
✅ /ws/v1/chat (WebSocket 핸드셰이크)
✅ SockJS fallback 지원
```

**평가:** 표준 STOMP 프로토콜을 사용하여 클라이언트와의 통신 구조가 명확합니다.

### 3. 메시지 흐름 아키텍처

```
Client
  → WebSocket (STOMP)
  → WebSocketChatController
  → ChatKafkaProducer
  → Kafka Topic (chat-messages)
  → ChatKafkaConsumer
  → ChatService (DB 저장 + WebSocket 브로드캐스트)
  → Client (구독자들에게 전달)
```

**구현 파일:**
- `WebSocketChatController.java:18-22` - 메시지 수신
- `ChatKafkaProducer.java:16-20` - **roomId를 key로 사용하여 메시지 순서 보장** ✅
- `ChatKafkaConsumer.java:16-20` - 메시지 구독 및 처리
- `ChatServiceImpl.java:20-28` - DB 저장 + 브로드캐스팅

**평가:** Kafka를 중간 계층으로 사용하여 확장성과 안정성을 확보한 우수한 설계입니다.

### 4. 채팅방 관리

#### 1:1 채팅
- `ChatRoomServiceImpl.getOrCreateOneToOneChatRoom` - 구현 완료 ✅
- 중복 채팅방 방지: `one_to_one_chat_rooms` 테이블의 UNIQUE 인덱스 활용 ✅
- 양방향 조회: `(userA, userB)` 또는 `(userB, userA)` 모두 검색 ✅

#### 그룹 채팅
- `ChatRoomServiceImpl.createGroupChatRoom` - 구현 완료 ✅
- 요청자를 포함한 모든 참여자 자동 초대 ✅

**평가:** 1:1 및 그룹 채팅의 기본 구조가 잘 구현되어 있습니다.

### 5. 데이터베이스 스키마

**DDL.md 기반 PostgreSQL 스키마:**
```sql
✅ chat_rooms (채팅방)
✅ one_to_one_chat_rooms (1:1 채팅방 매핑)
✅ chat_room_members (참여자)
✅ messages (메시지)
✅ message_deletions (개별 삭제)
✅ ENUM 타입 적극 활용 (chat_room_type, message_type)
✅ Soft Delete 지원
✅ UNIQUE 인덱스로 중복 방지
```

**평가:** 데이터베이스 설계가 PRD 요구사항을 완벽하게 반영하고 있습니다.

---

## ⚠️ 주요 문제점 및 개선 필요 사항

아래 내용은 2026-01-03 리뷰를 통해 업데이트된 현재 시스템의 주요 이슈 및 개선 권장 사항입니다. 이전에 CRITICAL로 분류되었던 `WebSocket 인증`과 `clientMessageId 중복 방지` 이슈는 해결되었습니다.

### 🔴 CRITICAL - 즉시 수정 필요

1.  **트랜잭셔널 Producer ID 미설정**
    - **문제:** `enable.idempotence`만 `true`로 설정되어 있고, 트랜잭션 ID가 없어 Kafka 메시지 전송 시 Exactly-Once를 완전히 보장하지 못합니다.
    - **해결:** `application.yml`에 `spring.kafka.producer.transaction-id-prefix`를 추가해야 합니다.

2.  **WebSocket senderId 검증 부족**
    - **문제:** 현재 클라이언트가 보내는 `senderId`를 그대로 사용하므로, 다른 사용자의 ID로 메시지를 보내는 어뷰징이 가능합니다.
    - **해결:** `WebSocketChatController`에서 메시지를 받을 때, STOMP 세션에 저장된 인증된 사용자 ID를 `senderId`에 강제로 할당해야 합니다.

3.  **메시지 발신 권한 검증 부족**
    - **문제:** 메시지를 보내는 사용자가 해당 채팅방의 멤버인지 확인하는 절차가 없습니다.
    - **해결:** `ChatService`의 메시지 처리 로직에서, 발신자가 채팅방 멤버인지 확인하는 검증 과정을 추가해야 합니다.

---

### 🟡 HIGH PRIORITY - 개선 권장

| 항목 | 현재 상태 | 필요 작업 |
|---|---|---|
| 읽음 처리 이벤트 | API만 있음 | Kafka `chat-read-events` 발행 로직 추가 |
| CORS 설정 | `*` 허용 | 프로덕션 환경에서는 특정 도메인만 허용하도록 수정 |
| 토큰 전달 방식 | URL 파라미터 | 보안 강화를 위해 Header/Cookie 방식 사용 권장 |
| DLT 알림 | 로그만 기록 | 실패 메시지 발생 시 Slack/Email 등 외부 알림 추가 |
| 메시지 유효성 검증 | 없음 | 메시지 길이 제한, 빈 메시지 등 유효성 검증 로직 추가 |

---

### 🟢 MEDIUM PRIORITY - 선택 사항

| 항목 | 현재 상태 | 필요 작업 |
|---|---|---|
| 구조화 로깅 | 기본 텍스트 로깅 | `logback-spring.xml`을 설정하여 JSON 포맷으로 로깅 |
| WebSocket 세션 제한 | 없음 | 메시지 크기, 버퍼, 타임아웃 등 세부 설정 추가 |
| Health Check 상세화 | 기본 설정만 사용 | Kubernetes 환경을 위해 readiness/liveness 프로브 분리 |
| 1:1 채팅 자기 자신 방지 | 없음 | 1:1 채팅방 생성 시 자기 자신과 채팅하는 경우를 방지 |
| ChatController TODO | 다수 존재 | 채팅방 나가기, 과거 메시지 조회 등 API 구현 |
| ChatRoomService 빈 구현 | getMyChatRooms | 내 채팅방 목록 조회 로직 구현 필요 |

---

## 📋 PRD 대비 구현 현황

| 기능 | PRD 요구사항 | 구현 상태 | 위치 | 비고 |
|------|-------------|----------|------|------|
| **Kafka 메시지 흐름** | Client → WebSocket → Kafka → Consumer → DB + Broadcast | ✅ 구현 | WebSocketChatController.java | |
| **메시지 순서 보장** | roomId를 Kafka Key로 사용 | ✅ 구현 | ChatKafkaProducer.java:19 | |
| **멱등성 Producer** | enable.idempotence=true | ✅ 구현 | application.yml:72 | |
| **트랜잭셔널 Producer** | transactional Producer 사용 | ❌ 미구현 | application.yml | **CRITICAL** |
| **clientMessageId 중복 방지** | UUID 기반 중복 체크 | ✅ 구현 | ChatServiceImpl.java | BIGINT 타입으로 변경 |
| **읽음 처리** | chat-read-events 토픽 | ❌ 미구현 | - | |
| **이미지 전송** | Azure SAS URL 연동 | ⚠️ 부분구현 | FileController.java | File 서비스는 있으나 채팅 통합 미완 |
| **그룹 채팅 이벤트** | INVITE, LEAVE, KICK | ❌ 미구현 | MessageType.java | Enum만 정의됨 |
| **WebSocket 인증** | Access Token 쿼리 파라미터 | ✅ 구현 | WebSocketConfig.java | `WebSocketAuthInterceptor` |
| **DLQ** | chat-messages-dlt | ✅ 구현 | KafkaConsumerConfig.java | `@DltHandler` |
| **Graceful Shutdown** | 30s timeout | ✅ 구현 | application.yml | `shutdown: graceful` |
| **Consumer Concurrency** | 3-5 (초기) | ✅ 구현 | application.yml | `listener.concurrency` |
| **토픽 명시적 생성** | chat-messages, chat-read-events | ❌ 미구현 | KafkaTopicConfig.java | |

**구현률: 9/13 (69.2%)**
**핵심 기능 구현률: 8/10 (80%)**

---

## 🎯 우선순위별 액션 플랜

### Phase 1: 즉시 수정 (1-2일) - MVP 출시 차단 이슈

**목표:** 보안 및 안정성 CRITICAL 이슈 해결

| 순번 | 작업 | 소요 시간 | 담당자 | 우선순위 |
|------|------|----------|--------|----------|
| 1 | WebSocket 인증 구현 | 2-3시간 | Backend | 🔴 CRITICAL |
| 2 | clientMessageId 중복 방지 로직 | 2-3시간 | Backend | 🔴 CRITICAL |
| 3 | 트랜잭셔널 Producer 설정 | 1-2시간 | Backend | 🔴 CRITICAL |

**총 소요 시간:** 5-8시간 (1일)

**완료 기준:**
- [ ] WebSocket 연결 시 JWT 토큰 검증
- [ ] 중복 메시지 저장 방지 (DB 에러 없이 처리)
- [ ] Kafka transactional.id 설정

---

### Phase 2: MVP 출시 전 (3-5일)

**목표:** 운영 안정성 및 모니터링 기반 구축

| 순번 | 작업 | 소요 시간 | 담당자 | 우선순위 |
|------|------|----------|--------|----------|
| 4 | DLQ 구현 | 3-4시간 | Backend | 🟡 HIGH |
| 5 | Graceful Shutdown 설정 | 10분 | Backend | 🟡 HIGH |
| 6 | Consumer Concurrency 설정 | 10분 | Backend | 🟡 HIGH |
| 7 | chat-messages 토픽 자동 생성 | 10분 | Backend | 🟡 HIGH |
| 8 | 구조화 로깅 (JSON) | 1-2시간 | Backend | 🟢 MEDIUM |

**총 소요 시간:** 5-7시간 (1일)

**완료 기준:**
- [ ] 메시지 처리 실패 시 DLT로 이동
- [ ] 배포 시 진행 중인 메시지 처리 완료 후 종료
- [ ] Consumer 3-5개 동시 실행
- [ ] Kafka 토픽 명시적 생성
- [ ] JSON 형식 로그 출력 (prod 프로필)

---

### Phase 3: 정식 서비스 전 (1-2주)

**목표:** 기능 완성도 및 UX 개선

| 순번 | 작업 | 소요 시간 | 담당자 | 우선순위 |
|------|------|----------|--------|----------|
| 9 | ChatController TODO 구현 | 2-3일 | Backend | 🟢 MEDIUM |
| 10 | 읽음 처리 로직 (chat-read-events) | 1-2일 | Backend | 🟢 MEDIUM |
| 11 | WebSocket 세션 제한 | 1-2시간 | Backend | 🟢 MEDIUM |
| 12 | Redis 패스워드 설정 (prod) | 10분 | Backend | 🟢 LOW |
| 13 | 그룹 채팅 이벤트 (INVITE, LEAVE, KICK) | 1일 | Backend | 🟢 MEDIUM |

**총 소요 시간:** 4-6일

**완료 기준:**
- [ ] 채팅방 나가기, 메시지 조회/삭제 API 완성
- [ ] 읽음 상태 실시간 업데이트
- [ ] 사용자당 최대 세션 수 제한
- [ ] 프로덕션 Redis 보안 설정

---

### Phase 4: 대규모 트래픽 대비 (장기)

**목표:** 성능 최적화 및 확장성 강화

| 순번 | 작업 | 소요 시간 | 담당자 | 우선순위 |
|------|------|----------|--------|----------|
| 14 | 서킷 브레이커 (Resilience4j) | 1-2일 | Backend | 🟢 LOW |
| 15 | 분산 추적 (Micrometer Tracing) | 1일 | Backend | 🟢 LOW |
| 16 | Spring Cache (Redis) | 1-2일 | Backend | 🟢 LOW |
| 17 | Kafka 파티션 수 조정 | 1일 | DevOps | 🟢 LOW |

**총 소요 시간:** 4-6일

**완료 기준:**
- [ ] FCM, Azure Blob 장애 대응
- [ ] 요청 흐름 추적 (Zipkin/Jaeger)
- [ ] 자주 조회되는 데이터 캐싱

---

## 🔧 즉시 적용 가능한 간단한 개선 사항

다음은 10분 이내에 바로 적용 가능한 설정들입니다:

### 1. Graceful Shutdown (10분)

**application.yml:**
```yaml
server:
  port: 28080
  shutdown: graceful  # 추가

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s  # 추가
```

### 2. Consumer Concurrency (10분)

**application.yml:**
```yaml
spring:
  kafka:
    listener:
      concurrency: 3
      ack-mode: record
```

### 3. 토픽 자동 생성 비활성화 (prod) (10분)

**application.yml - prod 프로필:**
```yaml
---
spring:
  config:
    activate:
      on-profile: prod
  kafka:
    properties:
      allow.auto.create.topics: false
```

### 4. Redis 패스워드 (prod) (10분)

**application.yml - prod 프로필:**
```yaml
---
spring:
  config:
    activate:
      on-profile: prod
  data:
    redis:
      password: ${REDIS_PASSWORD}
```

**총 소요 시간: 40분**

---

## 📊 구현 상태 체크리스트

### 인증 및 보안
- [x] Kafka SASL_SSL 설정
- [ ] WebSocket JWT 인증 ⚠️ **CRITICAL**
- [ ] WebSocket 세션 제한
- [ ] Redis 패스워드 (prod)

### Kafka 설정
- [x] enable.idempotence: true
- [x] isolation.level: read_committed
- [x] acks: all
- [ ] transaction-id-prefix ⚠️ **CRITICAL**
- [ ] DLQ 구현
- [ ] 토픽 명시적 생성

### 메시지 처리
- [x] roomId를 key로 순서 보장
- [ ] clientMessageId 중복 방지 로직 ⚠️ **CRITICAL**
- [ ] 읽음 처리 (chat-read-events)
- [ ] 메시지 조회 API
- [ ] 메시지 삭제 API

### 채팅방 관리
- [x] 1:1 채팅방 생성
- [x] 그룹 채팅방 생성
- [ ] 채팅방 목록 조회 (빈 구현)
- [ ] 채팅방 나가기
- [ ] 그룹 채팅 초대/강퇴
- [ ] 그룹 채팅 이벤트 (INVITE, LEAVE, KICK)

### 운영 및 모니터링
- [ ] Graceful Shutdown
- [ ] Consumer Concurrency
- [ ] 구조화 로깅 (JSON)
- [ ] 에러 알림 (Slack)
- [ ] 분산 추적 (Zipkin)

**완료: 6/26 (23.1%)**
**핵심 기능 완료: 4/10 (40%)**

---

## 🎓 참고 문서

### 내부 문서
- `docs/prd_draft.md` - 제품 요구사항 (채팅 섹션: 3.7)
- `docs/DDL.md` - 데이터베이스 스키마 (채팅 테이블: 7️⃣)
- `docs/production-checklist.md` - 프로덕션 체크리스트

### 외부 문서
- [Spring Kafka Documentation](https://docs.spring.io/spring-kafka/reference/)
- [Kafka Producer Configuration](https://kafka.apache.org/documentation/#producerconfigs)
- [Kafka Consumer Configuration](https://kafka.apache.org/documentation/#consumerconfigs)
- [Spring WebSocket STOMP](https://docs.spring.io/spring-framework/reference/web/websocket/stomp.html)

---

## ✅ 최종 결론

### 현재 상태 평가

**장점:**
- ✅ Kafka 기반의 확장 가능한 아키텍처
- ✅ 메시지 순서 보장 (roomId를 key로 사용)
- ✅ 기본적인 멱등성 설정
- ✅ 1:1 및 그룹 채팅 기본 구조 완성
- ✅ 데이터베이스 스키마 우수

**단점:**
- ❌ WebSocket 인증 없음 (보안 취약점)
- ❌ 메시지 중복 방지 로직 미완성
- ❌ 트랜잭셔널 Producer 미설정
- ❌ DLQ, Graceful Shutdown 등 운영 필수 설정 없음
- ❌ 많은 TODO 및 빈 구현

### MVP 출시 가능 여부

**현재:** ❌ **불가** (보안 이슈)

**Phase 1 완료 후:** ✅ **가능** (최소 3가지 CRITICAL 이슈 수정 필요)

**권장 출시 시점:** Phase 2 완료 후 (총 2-3일 소요)

### 1:1 및 그룹 채팅 지원 여부

✅ **기본 구조는 완성**되어 있으며, PRD 요구사항의 핵심 설계를 잘 따르고 있습니다.

**1:1 채팅:**
- ✅ 채팅방 생성/조회
- ✅ 실시간 메시지 전송
- ⚠️ 메시지 조회/삭제 미구현

**그룹 채팅:**
- ✅ 채팅방 생성
- ✅ 실시간 메시지 전송
- ⚠️ 초대/강퇴/이벤트 미구현

### 다음 단계

1. **즉시 (1-2일):** Phase 1 완료 → 보안 및 안정성 확보
2. **MVP 출시 전 (3-5일):** Phase 2 완료 → 운영 기반 구축
3. **정식 서비스 전 (1-2주):** Phase 3 완료 → 기능 완성도 향상
4. **대규모 대비 (장기):** Phase 4 완료 → 성능 최적화

---

**문서 버전:** 1.0
**최종 업데이트:** 2025-12-29
**다음 검토 예정일:** Phase 1 완료 후

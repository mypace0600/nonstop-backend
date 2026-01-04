# 채팅 읽음 처리 이벤트 설계 문서

**버전:** v1.0
**작성일:** 2026-01-04
**상태:** Draft

---

## 1. 개요

### 1.1 목적
채팅 메시지의 읽음 상태를 Kafka 기반 비동기 이벤트로 처리하여, 메인 채팅 흐름에 영향을 주지 않으면서 `last_read_message_id`와 `unread_count`를 관리한다.

### 1.2 현재 상태
| 항목 | 상태 | 위치 |
|------|------|------|
| 읽음 처리 API | ✅ 있음 | `ChatController.java:72-79` |
| DB 업데이트 | ✅ 있음 | `ChatRoomServiceImpl.markAsRead()` |
| Kafka 이벤트 발행 | ❌ 없음 | - |
| unread_count 관리 | ❌ 없음 | - |
| 실시간 읽음 상태 브로드캐스트 | ❌ 없음 | - |

### 1.3 목표
```
Before: API 호출 → DB 직접 업데이트 (동기)
After:  API 호출 → Kafka 발행 → Consumer → DB 업데이트 + WebSocket 브로드캐스트 (비동기)
```

---

## 2. 아키텍처

### 2.1 전체 흐름

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           읽음 처리 이벤트 흐름                              │
└─────────────────────────────────────────────────────────────────────────────┘

[Client]
    │
    ├─── (1) 채팅방 진입 시 ───────────────────────────────────────────────────┐
    │         PATCH /api/v1/chat/rooms/{roomId}/read?messageId={lastMsgId}    │
    │                                                                          │
    ├─── (2) WebSocket 메시지 수신 시 (선택) ──────────────────────────────────┤
    │         클라이언트가 자동으로 읽음 처리 API 호출                          │
    │                                                                          │
    ▼                                                                          │
[ChatController]                                                               │
    │                                                                          │
    │  markAsRead(roomId, userId, messageId)                                   │
    │                                                                          │
    ▼                                                                          │
[ChatReadEventProducer]                                                        │
    │                                                                          │
    │  ┌─────────────────────────────────────────┐                             │
    │  │  ChatReadEventDto                       │                             │
    │  │  - roomId: Long                         │                             │
    │  │  - userId: Long                         │                             │
    │  │  - messageId: Long                      │                             │
    │  │  - timestamp: LocalDateTime             │                             │
    │  └─────────────────────────────────────────┘                             │
    │                                                                          │
    │  Kafka Key: userId (사용자별 순서 보장)                                   │
    │                                                                          │
    ▼                                                                          │
[Kafka Topic: chat-read-events]                                                │
    │                                                                          │
    │  파티션: 5~10개                                                          │
    │  Retention: 1~3일                                                        │
    │                                                                          │
    ▼                                                                          │
[ChatReadEventConsumer]                                                        │
    │                                                                          │
    ├─── (A) DB 업데이트                                                       │
    │         UPDATE chat_room_members                                         │
    │         SET last_read_message_id = ?                                     │
    │         WHERE room_id = ? AND user_id = ?                                │
    │                                                                          │
    └─── (B) WebSocket 브로드캐스트 (선택)                                      │
              /sub/chat/room/{roomId}/read                                     │
              { userId, messageId, timestamp }                                 │
                                                                               │
              → 다른 참여자에게 읽음 상태 실시간 알림                           │
```

### 2.2 컴포넌트 구조

```
com.app.nonstop.domain.chat
├── controller
│   └── ChatController.java          # 기존 (수정)
├── dto
│   ├── ChatMessageDto.java          # 기존
│   └── ChatReadEventDto.java        # 신규
├── service
│   ├── ChatKafkaProducer.java       # 기존 (수정: 제네릭화)
│   ├── ChatKafkaConsumer.java       # 기존
│   ├── ChatReadEventProducer.java   # 신규
│   ├── ChatReadEventConsumer.java   # 신규
│   ├── ChatRoomService.java         # 기존
│   └── ChatRoomServiceImpl.java     # 기존 (수정)
└── mapper
    └── ChatRoomMapper.java          # 기존 (수정 가능)
```

---

## 3. 상세 설계

### 3.1 DTO 설계

#### ChatReadEventDto.java (신규)
```java
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatReadEventDto {
    private Long roomId;
    private Long userId;
    private Long messageId;
    private LocalDateTime timestamp;
}
```

#### ChatReadStatusDto.java (신규 - WebSocket 브로드캐스트용)
```java
@Getter
@Builder
public class ChatReadStatusDto {
    private Long roomId;
    private Long userId;
    private Long lastReadMessageId;
    private LocalDateTime readAt;
}
```

### 3.2 Producer 설계

#### ChatReadEventProducer.java (신규)
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatReadEventProducer {

    private static final String TOPIC = "chat-read-events";
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendReadEvent(ChatReadEventDto event) {
        log.info("Sending read event to Kafka: roomId={}, userId={}, messageId={}",
                event.getRoomId(), event.getUserId(), event.getMessageId());

        // userId를 key로 사용하여 동일 사용자의 읽음 이벤트 순서 보장
        kafkaTemplate.send(TOPIC, String.valueOf(event.getUserId()), event);
    }
}
```

### 3.3 Consumer 설계

#### ChatReadEventConsumer.java (신규)
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatReadEventConsumer {

    private final ChatRoomMapper chatRoomMapper;
    private final SimpMessageSendingOperations messagingTemplate;

    @KafkaListener(
        topics = "chat-read-events",
        groupId = "nonstop-chat-read",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ChatReadEventDto event) {
        log.info("Consumed read event: roomId={}, userId={}, messageId={}",
                event.getRoomId(), event.getUserId(), event.getMessageId());

        try {
            // 1. DB 업데이트 (last_read_message_id)
            chatRoomMapper.updateLastReadMessageId(
                event.getRoomId(),
                event.getUserId(),
                event.getMessageId()
            );

            // 2. WebSocket으로 읽음 상태 브로드캐스트 (선택)
            broadcastReadStatus(event);

            log.info("Read event processed successfully: roomId={}, userId={}",
                    event.getRoomId(), event.getUserId());

        } catch (Exception e) {
            log.error("Failed to process read event: roomId={}, userId={}, error={}",
                    event.getRoomId(), event.getUserId(), e.getMessage());
            throw e; // DLT로 이동
        }
    }

    private void broadcastReadStatus(ChatReadEventDto event) {
        ChatReadStatusDto status = ChatReadStatusDto.builder()
                .roomId(event.getRoomId())
                .userId(event.getUserId())
                .lastReadMessageId(event.getMessageId())
                .readAt(event.getTimestamp())
                .build();

        // 해당 채팅방의 다른 참여자들에게 읽음 상태 알림
        messagingTemplate.convertAndSend(
            "/sub/chat/room/" + event.getRoomId() + "/read",
            status
        );
    }

    @DltHandler
    public void handleDlt(ChatReadEventDto event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("DLT - Failed to process read event: topic={}, roomId={}, userId={}",
                topic, event.getRoomId(), event.getUserId());
    }
}
```

### 3.4 Service 수정

#### ChatRoomServiceImpl.java (수정)
```java
// Before
@Override
@Transactional
public void markAsRead(Long roomId, Long userId, Long messageId) {
    chatRoomMapper.updateLastReadMessageId(roomId, userId, messageId);
}

// After
@Override
public void markAsRead(Long roomId, Long userId, Long messageId) {
    // 멤버 여부 검증
    if (!chatRoomMapper.isMemberOfRoom(roomId, userId)) {
        throw new AccessDeniedException("You are not a member of this chat room");
    }

    // Kafka로 읽음 이벤트 발행 (비동기 처리)
    ChatReadEventDto event = ChatReadEventDto.builder()
            .roomId(roomId)
            .userId(userId)
            .messageId(messageId)
            .timestamp(LocalDateTime.now())
            .build();

    chatReadEventProducer.sendReadEvent(event);
}
```

---

## 4. Kafka 토픽 설정

### 4.1 토픽 구성

| 토픽명 | 파티션 수 | Replication | Retention | Key |
|--------|----------|-------------|-----------|-----|
| `chat-read-events` | 5 | 3 | 1일 | userId |

### 4.2 KafkaTopicConfig.java (신규 또는 수정)
```java
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic chatReadEventsTopic() {
        return TopicBuilder.name("chat-read-events")
                .partitions(5)
                .replicas(3)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(1).toMillis()))
                .build();
    }
}
```

---

## 5. WebSocket 구독 엔드포인트

### 5.1 기존 엔드포인트
| 용도 | 엔드포인트 |
|------|-----------|
| 메시지 수신 | `/sub/chat/room/{roomId}` |

### 5.2 신규 엔드포인트
| 용도 | 엔드포인트 | Payload |
|------|-----------|---------|
| 읽음 상태 수신 | `/sub/chat/room/{roomId}/read` | `ChatReadStatusDto` |

### 5.3 클라이언트 구독 예시
```javascript
// 메시지 구독 (기존)
stompClient.subscribe('/sub/chat/room/123', (message) => {
    const chatMessage = JSON.parse(message.body);
    displayMessage(chatMessage);
});

// 읽음 상태 구독 (신규)
stompClient.subscribe('/sub/chat/room/123/read', (message) => {
    const readStatus = JSON.parse(message.body);
    updateReadStatus(readStatus.userId, readStatus.lastReadMessageId);
});
```

---

## 6. Unread Count 관리 전략

### 6.1 옵션 비교

| 옵션 | 설명 | 장점 | 단점 |
|------|------|------|------|
| **A. 실시간 계산** | API 호출 시 쿼리로 계산 | 항상 정확 | DB 부하 |
| **B. 캐시 기반** | Redis에 unread_count 저장 | 빠름 | 동기화 이슈 |
| **C. 비정규화** | chat_room_members에 컬럼 추가 | 조회 빠름 | 업데이트 복잡 |

### 6.2 권장안: A. 실시간 계산 (MVP)

MVP 단계에서는 실시간 계산 방식을 사용하고, 트래픽 증가 시 캐시 기반으로 전환합니다.

```sql
-- unread_count 계산 쿼리
SELECT COUNT(*)
FROM messages m
WHERE m.chat_room_id = :roomId
  AND m.id > COALESCE(
      (SELECT last_read_message_id
       FROM chat_room_members
       WHERE room_id = :roomId AND user_id = :userId),
      0
  )
  AND m.sender_id != :userId;
```

### 6.3 ChatRoomMapper 수정

```java
// 신규 메서드
int countUnreadMessages(@Param("roomId") Long roomId, @Param("userId") Long userId);
```

```xml
<!-- ChatRoomMapper.xml -->
<select id="countUnreadMessages" resultType="int">
    SELECT COUNT(*)
    FROM messages m
    WHERE m.chat_room_id = #{roomId}
      AND m.id > COALESCE(
          (SELECT last_read_message_id
           FROM chat_room_members
           WHERE room_id = #{roomId} AND user_id = #{userId}),
          0
      )
      AND m.sender_id != #{userId}
</select>
```

---

## 7. 시퀀스 다이어그램

### 7.1 채팅방 진입 시 읽음 처리

```
┌────────┐          ┌──────────────┐          ┌───────────────────┐          ┌───────┐          ┌────────────────────┐
│ Client │          │ChatController│          │ChatRoomServiceImpl│          │ Kafka │          │ChatReadEventConsumer│
└───┬────┘          └──────┬───────┘          └─────────┬─────────┘          └───┬───┘          └──────────┬─────────┘
    │                      │                            │                        │                         │
    │ PATCH /rooms/{id}/read                            │                        │                         │
    │ ?messageId=100       │                            │                        │                         │
    │─────────────────────>│                            │                        │                         │
    │                      │                            │                        │                         │
    │                      │ markAsRead(roomId, userId, messageId)               │                         │
    │                      │───────────────────────────>│                        │                         │
    │                      │                            │                        │                         │
    │                      │                            │ sendReadEvent(event)   │                         │
    │                      │                            │───────────────────────>│                         │
    │                      │                            │                        │                         │
    │                      │<───────────────────────────│                        │                         │
    │    200 OK            │                            │                        │                         │
    │<─────────────────────│                            │                        │                         │
    │                      │                            │                        │                         │
    │                      │                            │                        │  consume(event)         │
    │                      │                            │                        │────────────────────────>│
    │                      │                            │                        │                         │
    │                      │                            │                        │         updateLastReadMessageId()
    │                      │                            │                        │         broadcastReadStatus()
    │                      │                            │                        │                         │
    │                      │                            │     /sub/chat/room/{roomId}/read                 │
    │<────────────────────────────────────────────────────────────────────────────────────────────────────│
    │                      │                            │                        │                         │
```

---

## 8. 에러 처리

### 8.1 Producer 실패
- Kafka 연결 실패 시 로그 기록
- API는 202 Accepted 반환 (비동기 처리이므로)
- 재시도 정책: 3회

### 8.2 Consumer 실패
- DLT(Dead Letter Topic)로 이동
- `chat-read-events-dlt` 토픽에 실패 메시지 저장
- 모니터링 알림 발송 (TODO)

### 8.3 멱등성 보장
- 동일한 (roomId, userId, messageId) 조합에 대해 여러 번 처리해도 결과 동일
- `last_read_message_id`는 항상 더 큰 값으로만 업데이트

```sql
UPDATE chat_room_members
SET last_read_message_id = :messageId
WHERE room_id = :roomId
  AND user_id = :userId
  AND (last_read_message_id IS NULL OR last_read_message_id < :messageId);
```

---

## 9. 모니터링

### 9.1 주요 메트릭

| 메트릭 | 설명 | 알림 임계값 |
|--------|------|------------|
| Consumer Lag | 처리 지연 메시지 수 | > 1000 |
| Processing Time | 이벤트 처리 시간 | > 500ms |
| Error Rate | 실패율 | > 1% |

### 9.2 로그 포맷

```
INFO  - Sending read event to Kafka: roomId={}, userId={}, messageId={}
INFO  - Consumed read event: roomId={}, userId={}, messageId={}
INFO  - Read event processed successfully: roomId={}, userId={}
ERROR - Failed to process read event: roomId={}, userId={}, error={}
ERROR - DLT - Failed to process read event: topic={}, roomId={}, userId={}
```

---

## 10. 구현 체크리스트

### Phase 1: 기본 구현
- [ ] `ChatReadEventDto` 생성
- [ ] `ChatReadEventProducer` 생성
- [ ] `ChatReadEventConsumer` 생성
- [ ] `ChatRoomServiceImpl.markAsRead()` 수정
- [ ] Kafka 토픽 설정 (chat-read-events)

### Phase 2: WebSocket 브로드캐스트
- [ ] `ChatReadStatusDto` 생성
- [ ] Consumer에서 브로드캐스트 로직 추가
- [ ] 클라이언트 구독 문서화

### Phase 3: Unread Count
- [ ] `countUnreadMessages` 쿼리 추가
- [ ] 채팅방 목록 API에 unread_count 포함
- [ ] (선택) Redis 캐시 도입

### Phase 4: 모니터링
- [ ] Consumer lag 모니터링
- [ ] DLT 알림 연동
- [ ] Grafana 대시보드

---

## 11. 참고

### 11.1 PRD 요구사항 (섹션 3.7.2)
> 사용자가 채팅방에 진입하거나 메시지를 수신하는 시점에 '읽음' 이벤트를 별도의 Kafka 토픽(`chat-read-events`)으로 발행합니다. 이 토픽을 구독하는 전용 Consumer가 `last_read_message_id` 및 `unread_count` 업데이트를 처리합니다.

### 11.2 관련 파일
- `ChatController.java:72-79` - 읽음 처리 API
- `ChatRoomServiceImpl.java:117-120` - 현재 markAsRead 구현
- `ChatRoomMapper.java:20` - updateLastReadMessageId

### 11.3 Kafka 토픽 정책 (PRD)
- 파티션 수: 5~10개 (userId 키 기반)
- Replication factor: 최소 3
- Retention: 1~3일

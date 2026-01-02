# Kafka & WebSocket 기반 실시간 채팅 시스템 종합 검토 리포트

**작성일:** 2025-12-29
**버전:** v1.0
**검토 범위:** Kafka 설정, WebSocket 구현, 채팅 기능 (1:1 및 그룹)

---

## 📊 전체 요약

**Kafka와 WebSocket 기반의 실시간 채팅 시스템은 전반적으로 우수한 아키텍처**를 가지고 있습니다. PRD 문서의 핵심 설계 원칙을 잘 따르고 있으며, 메시지 순서 보장, 멱등성 등 중요한 부분들이 잘 구현되어 있습니다.

### 종합 점수

| 항목 | 점수 | 평가 |
|------|------|------|
| **아키텍처 설계** | 85/100 | Kafka 기반 설계는 우수하나 세부 구현 미흡 |
| **보안** | 60/100 | WebSocket 인증 없음 (CRITICAL) |
| **안정성** | 70/100 | DLQ, Graceful Shutdown 없음 |
| **확장성** | 90/100 | Kafka 기반으로 우수한 확장성 |
| **운영 준비도** | 65/100 | 모니터링, 로깅 개선 필요 |

**총점: 74/100**

### MVP 출시 가능 여부

**현재 상태:** ❌ 보안 이슈로 출시 불가
**수정 후:** ✅ 3가지 CRITICAL 이슈 수정 후 출시 가능

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

### 🔴 CRITICAL - 즉시 수정 필요

#### 1. WebSocket 인증 미구현 ⚠️⚠️⚠️

**문제:**
현재 WebSocket 연결 시 인증이 전혀 없어 누구나 접근 가능한 보안 취약점이 존재합니다.

**현재 코드 (WebSocketConfig.java:24-26):**
```java
registry.addEndpoint("/ws/v1/chat")
        .setAllowedOriginPatterns("*")  // ❌ 누구나 접근 가능
        .withSockJS();
```

**PRD 요구사항 (prd_draft.md:101-102):**
> - `wss://api.nonstop.app/ws/v1/chat`
> - 연결 시 Access Token 쿼리 파라미터로 인증

**해결 방법:**

```java
package com.app.nonstop.global.config;

import com.app.nonstop.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/sub");
        registry.setApplicationDestinationPrefixes("/pub");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/v1/chat")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // Authorization 헤더에서 토큰 추출
                    String token = accessor.getFirstNativeHeader("Authorization");

                    if (token != null && token.startsWith("Bearer ")) {
                        token = token.substring(7);
                    }

                    // 토큰 검증
                    if (token == null || !jwtTokenProvider.validateToken(token)) {
                        throw new IllegalArgumentException("Invalid or missing JWT token");
                    }

                    // 사용자 ID 추출 및 설정
                    Long userId = jwtTokenProvider.getUserIdFromToken(token);
                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                        userId, null, null
                    );
                    accessor.setUser(authentication);
                }

                return message;
            }
        });
    }
}
```

**영향도:** 🔴 매우 높음 (보안 취약점)
**소요 시간:** 2-3시간

---

#### 2. clientMessageId 중복 방지 로직 없음

**문제:**
DTO에 `clientMessageId` 필드는 있지만, 실제 중복 체크 로직이 없어 네트워크 재시도 시 메시지 중복 저장 가능성이 있습니다.

**현재 상태:**
- `ChatMessageDto.java:23` - clientMessageId 필드 정의됨 ✅
- `DDL.md:356-357` - DB UNIQUE 인덱스 있음 ✅
- `ChatMapper.xml:7-8` - INSERT 시 clientMessageId 포함 ✅
- **문제:** DB 제약조건 위반 시 500 에러 발생 → 클라이언트에게 에러 전파 ❌

**PRD 요구사항 (prd_draft.md:116-120):**
> - 클라이언트는 메시지 전송 시 **`clientMessageId` (UUID)**를 생성하여 Payload에 포함
> - `ChatKafkaConsumer`는 메시지 수신 후 DB 저장 시 `clientMessageId`를 함께 저장
> - 효과: Kafka의 exactly-once semantics를 강화하여 네트워크 재시도로 인한 중복을 완벽하게 방지

**해결 방법:**

**1) ChatMapper.java에 메서드 추가:**
```java
package com.app.nonstop.mapper;

import com.app.nonstop.domain.chat.dto.ChatMessageDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.UUID;

@Mapper
public interface ChatMapper {
    void insertMessage(ChatMessageDto message);

    // 추가: clientMessageId 중복 체크
    boolean existsByClientMessageId(@Param("clientMessageId") UUID clientMessageId);
}
```

**2) ChatMapper.xml에 쿼리 추가:**
```xml
<select id="existsByClientMessageId" resultType="boolean">
    SELECT EXISTS(
        SELECT 1 FROM messages
        WHERE client_message_id = #{clientMessageId}
    )
</select>
```

**3) ChatServiceImpl.java 수정:**
```java
package com.app.nonstop.domain.chat.service;

import com.app.nonstop.domain.chat.dto.ChatMessageDto;
import com.app.nonstop.mapper.ChatMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatMapper chatMapper;

    @Override
    @Transactional
    public void saveAndBroadcastMessage(ChatMessageDto message) {
        try {
            // 1. clientMessageId 중복 체크 (있는 경우만)
            if (message.getClientMessageId() != null) {
                boolean exists = chatMapper.existsByClientMessageId(message.getClientMessageId());
                if (exists) {
                    log.warn("Duplicate message detected, skipping: clientMessageId={}",
                        message.getClientMessageId());
                    return; // 중복 메시지는 저장하지 않고 무시
                }
            }

            // 2. 메시지 DB 저장
            message.setSentAt(LocalDateTime.now());
            chatMapper.insertMessage(message);

            // 3. WebSocket으로 메시지 브로드캐스팅
            messagingTemplate.convertAndSend("/sub/chat/room/" + message.getRoomId(), message);

            log.info("Message saved and broadcasted: messageId={}, roomId={}",
                message.getMessageId(), message.getRoomId());

        } catch (DuplicateKeyException e) {
            // DB UNIQUE 제약조건 위반 (동시성 이슈로 발생 가능)
            log.warn("Duplicate message insert attempt (race condition): clientMessageId={}",
                message.getClientMessageId());
            // 이미 저장된 경우이므로 무시
        }
    }
}
```

**영향도:** 🔴 높음 (메시지 중복 가능)
**소요 시간:** 2-3시간

---

#### 3. 트랜잭셔널 Producer 미설정

**문제:**
멱등성(idempotence)만 설정되어 있고, 트랜잭셔널 Producer가 설정되지 않아 exactly-once 보장이 완전하지 않습니다.

**PRD 요구사항 (prd_draft.md:118-119):**
> - Kafka Producer 설정: `enable.idempotence=true`를 활성화하고, **트랜잭셔널 Producer를 사용**하여 원자적인 쓰기 작업을 보장

**현재 상태 (application.yml:66-73):**
```yaml
producer:
  acks: all
  retries: 3
  properties:
    enable.idempotence: true  # ✅ 설정됨
    # ❌ transactional.id 없음
```

**해결 방법:**

**1) application.yml 수정:**
```yaml
spring:
  kafka:
    producer:
      transaction-id-prefix: tx-nonstop-chat-
      acks: all
      retries: 3
      properties:
        enable.idempotence: true
        max.in.flight.requests.per.connection: 5
```

**2) KafkaProducerConfig.java 수정 (선택사항):**
```java
package com.app.nonstop.global.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // 멱등성 프로듀서 설정
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        // 트랜잭셔널 ID (application.yml에서 설정하므로 여기서는 선택사항)
        // configProps.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "tx-nonstop-producer");

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

**영향도:** 🟡 중간 (exactly-once 보장 강화)
**소요 시간:** 1-2시간

---

### 🟡 HIGH PRIORITY - 빠른 시일 내 개선 필요

#### 4. DLQ (Dead Letter Queue) 없음

**문제:**
메시지 처리 실패 시 재시도 후 DLQ로 이동시키는 로직이 없어, 영구적으로 실패한 메시지가 손실될 수 있습니다.

**PRD 요구사항 (prd_draft.md:146):**
> - **에러 핸들링**: Dead Letter Topic (DLQ) 추가 – 실패 메시지 라우팅

**production-checklist.md (116-209)에 상세한 구현 방법 있음**

**해결 방법:**

**1) KafkaTopicConfig.java 생성:**
```java
package com.app.nonstop.global.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic chatMessagesTopic() {
        return TopicBuilder.name("chat-messages")
            .partitions(10)
            .replicas(3)
            .build();
    }

    @Bean
    public NewTopic chatMessagesDltTopic() {
        return TopicBuilder.name("chat-messages-dlt")
            .partitions(3)
            .replicas(3)
            .build();
    }

    @Bean
    public NewTopic chatReadEventsTopic() {
        return TopicBuilder.name("chat-read-events")
            .partitions(5)
            .replicas(3)
            .build();
    }
}
```

**2) KafkaConsumerConfig.java 수정:**
```java
package com.app.nonstop.global.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            KafkaTemplate<String, Object> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());

        // DLQ로 보내는 DeadLetterPublishingRecoverer
        DeadLetterPublishingRecoverer recoverer =
            new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + "-dlt", record.partition()));

        // 3번 재시도 후 DLQ로 이동 (1초 간격)
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
            recoverer,
            new FixedBackOff(1000L, 3L)
        );

        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
```

**3) DLT Handler 추가 (ChatKafkaConsumer.java):**
```java
package com.app.nonstop.domain.chat.service;

import com.app.nonstop.domain.chat.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatKafkaConsumer {

    private final ChatService chatService;

    @KafkaListener(topics = "chat-messages", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(ChatMessageDto message) {
        log.info("Consumed message from Kafka: {}", message);
        chatService.saveAndBroadcastMessage(message);
    }

    @DltHandler
    public void handleDlt(ChatMessageDto message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("DLT 메시지 수신 - 처리 실패한 메시지: topic={}, roomId={}, senderId={}, content={}",
            topic, message.getRoomId(), message.getSenderId(), message.getContent());

        // TODO: 관리자 알림 전송 (Slack, Email 등)
        // TODO: DB에 실패 로그 저장
    }
}
```

**영향도:** 🟡 중간 (운영 안정성)
**소요 시간:** 3-4시간

---

#### 5. Graceful Shutdown 미설정

**문제:**
배포 시 진행 중인 요청과 Kafka 메시지 처리가 중단되어 메시지 손실 가능성이 있습니다.

**production-checklist.md (40-56):**
> 배포 시 진행 중인 요청과 Kafka 메시지 처리를 안전하게 완료하기 위해 필수.

**해결 방법:**

**application.yml에 추가:**
```yaml
server:
  port: 28080
  shutdown: graceful  # 추가

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s  # 추가
```

**효과:**
- 배포 시 새 요청 거부, 기존 요청 처리 완료 대기
- Kafka Consumer가 현재 처리 중인 메시지 완료 후 종료
- 최대 30초 대기 후 강제 종료

**영향도:** 🟡 중간 (운영 안정성)
**소요 시간:** 10분

---

#### 6. Consumer Concurrency 미설정

**문제:**
Consumer가 1개만 실행되어 처리량이 제한됩니다.

**production-checklist.md (92-113):**
> 파티션 수에 맞는 동시 처리 설정.

**해결 방법:**

**application.yml에 추가:**
```yaml
spring:
  kafka:
    listener:
      concurrency: ${KAFKA_CONSUMER_CONCURRENCY:3}
      ack-mode: record  # 메시지별 ACK (안전)
      # ack-mode: batch  # 배치 ACK (성능 우선 시)
```

**설정 가이드:**
| 파티션 수 | 권장 concurrency | 비고 |
|-----------|-----------------|------|
| 10 | 3~5 | 초기 서비스 |
| 30 | 10~15 | 성장기 |
| 50+ | 20~30 | 대규모 |

**주의:** concurrency > 파티션 수면 일부 Consumer가 놀게 됨

**영향도:** 🟡 중간 (성능)
**소요 시간:** 10분

---

#### 7. 읽음 처리 로직 없음

**문제:**
채팅방의 읽음 상태(`last_read_message_id`, `unread_count`) 업데이트 로직이 구현되지 않았습니다.

**PRD 요구사항 (prd_draft.md:122-124):**
> - **읽음 처리 전략**: 사용자가 채팅방에 진입하거나 메시지를 수신하는 시점에 '읽음' 이벤트를 별도의 Kafka 토픽(`chat-read-events`)으로 발행

**현재 상태:**
- DDL에 `last_read_message_id` 컬럼 정의됨 ✅
- 실제 업데이트 로직 없음 ❌
- `chat-read-events` 토픽 없음 ❌

**해결 방법:**

**Phase 3에서 구현 권장 (1-2주 내)**

**영향도:** 🟢 중간 (UX)
**소요 시간:** 1-2일

---

#### 8. chat-messages 토픽 생성 설정 없음

**문제:**
프로덕션에서는 토픽을 명시적으로 관리해야 하나, 현재 자동 생성에 의존하고 있습니다.

**해결 방법:**
위 **#4 DLQ** 섹션의 `KafkaTopicConfig.java` 참고

**프로덕션 설정 추가:**
```yaml
# application.yml - prod 프로필
spring:
  kafka:
    properties:
      allow.auto.create.topics: false
```

**영향도:** 🟢 낮음 (운영 정책)
**소요 시간:** 10분 (#4와 함께 처리)

---

### 🟢 MEDIUM PRIORITY - 프로덕션 전 권장

#### 9. WebSocket 세션 제한 없음

**문제:**
사용자당 무제한 WebSocket 연결을 허용하여 리소스 고갈 가능성이 있습니다.

**production-checklist.md (382-430) 참고**

**해결 방법:**

**WebSocketConfig.java에 추가:**
```java
@Override
public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
    registry.setMessageSizeLimit(64 * 1024);      // 64KB 메시지 크기 제한
    registry.setSendBufferSizeLimit(512 * 1024);  // 512KB 버퍼 제한
    registry.setSendTimeLimit(20 * 1000);         // 20초 전송 타임아웃
}
```

**영향도:** 🟢 낮음
**소요 시간:** 1-2시간

---

#### 10. Redis 패스워드 없음 (local은 OK, prod 필요)

**현재 상태 (application.yml:49-52):**
```yaml
data:
  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6379}
    # ❌ password 없음
```

**해결 방법:**

**application.yml - prod 프로필 추가:**
```yaml
---
spring:
  config:
    activate:
      on-profile: prod
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD}
      ssl:
        enabled: true  # Azure Cache for Redis 사용 시
```

**영향도:** 🟢 낮음 (프로덕션에서만 필요)
**소요 시간:** 10분

---

#### 11. 구조화 로깅 없음

**문제:**
현재 일반 텍스트 로그를 사용하여 로그 수집 시스템(ELK, Azure Monitor) 연동이 어렵습니다.

**production-checklist.md (230-277):** Logstash JSON 로깅 가이드 참고

**해결 방법:**

**build.gradle에 의존성 추가:**
```gradle
implementation 'net.logstash.logback:logstash-logback-encoder:7.4'
```

**src/main/resources/logback-spring.xml 생성:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <springProfile name="local">
        <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
        <include resource="org/springframework/boot/logging/logback/console-appender.xml"/>
        <root level="INFO">
            <appender-ref ref="CONSOLE"/>
        </root>
    </springProfile>

    <springProfile name="prod">
        <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
            <encoder class="net.logstash.logback.encoder.LogstashEncoder">
                <includeMdcKeyName>traceId</includeMdcKeyName>
                <includeMdcKeyName>userId</includeMdcKeyName>
                <customFields>{"app":"nonstop","env":"prod"}</customFields>
            </encoder>
        </appender>
        <root level="INFO">
            <appender-ref ref="JSON_CONSOLE"/>
        </root>
    </springProfile>
</configuration>
```

**영향도:** 🟢 낮음 (모니터링 개선)
**소요 시간:** 1-2시간

---

#### 12. ChatController TODO 많음

**ChatController.java:60-66:**
```java
// TODO: 채팅방 나가기 (DELETE /api/v1/chat/rooms/{roomId})
// TODO: 과거 메시지 조회 (GET /api/v1/chat/rooms/{roomId}/messages)
// TODO: 나에게만 메시지 삭제 (DELETE /api/v1/chat/rooms/{roomId}/messages/{msgId})
// TODO: 그룹 채팅방 정보 수정 (PATCH /api/v1/chat/group-rooms/{roomId})
// TODO: 그룹 채팅방 참여자 목록 조회 (GET /api/v1/chat/group-rooms/{roomId}/members)
// TODO: 그룹 채팅방에 사용자 초대 (POST /api/v1/chat/group-rooms/{roomId}/invite)
// TODO: 그룹 채팅방에서 사용자 강퇴 (DELETE /api/v1/chat/group-rooms/{roomId}/members/{userId})
```

**영향도:** 🟢 중간 (기능 완성도)
**소요 시간:** 2-3일

---

#### 13. ChatRoomService.getMyChatRooms 빈 구현

**ChatRoomServiceImpl.java:28-33:**
```java
@Override
public List<ChatRoomResponseDto> getMyChatRooms(Long userId) {
    // TODO: Implement logic to retrieve chat rooms for the given user
    return List.of();
}
```

**영향도:** 🟢 중간
**소요 시간:** 4-6시간

---

## 📋 PRD 대비 구현 현황

| 기능 | PRD 요구사항 | 구현 상태 | 위치 | 비고 |
|------|-------------|----------|------|------|
| **Kafka 메시지 흐름** | Client → WebSocket → Kafka → Consumer → DB + Broadcast | ✅ 구현 | WebSocketChatController.java | |
| **메시지 순서 보장** | roomId를 Kafka Key로 사용 | ✅ 구현 | ChatKafkaProducer.java:19 | |
| **멱등성 Producer** | enable.idempotence=true | ✅ 구현 | application.yml:72 | |
| **트랜잭셔널 Producer** | transactional Producer 사용 | ❌ 미구현 | application.yml | 추가 필요 |
| **clientMessageId 중복 방지** | UUID 기반 중복 체크 | ⚠️ 부분구현 | ChatServiceImpl.java | DB 인덱스만 있고 로직 없음 |
| **읽음 처리** | chat-read-events 토픽 | ❌ 미구현 | - | |
| **이미지 전송** | Azure SAS URL 연동 | ⚠️ 부분구현 | FileController.java | File 서비스는 있으나 채팅 통합 미완 |
| **그룹 채팅 이벤트** | INVITE, LEAVE, KICK | ❌ 미구현 | MessageType.java | Enum만 정의됨 |
| **WebSocket 인증** | Access Token 쿼리 파라미터 | ❌ 미구현 | WebSocketConfig.java | **CRITICAL** |
| **DLQ** | chat-messages-dlt | ❌ 미구현 | KafkaConsumerConfig.java | |
| **Graceful Shutdown** | 30s timeout | ❌ 미구현 | application.yml | |
| **Consumer Concurrency** | 3-5 (초기) | ❌ 미구현 | application.yml | |
| **토픽 명시적 생성** | chat-messages, chat-read-events | ❌ 미구현 | KafkaTopicConfig.java | |

**구현률: 5/13 (38.5%)**
**핵심 기능 구현률: 4/6 (66.7%)**

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

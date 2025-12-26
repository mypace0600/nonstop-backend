# Nonstop App - Production Checklist

**Version:** v1.0
**Last Updated:** 2025.12.26
**Status:** MVP 준비 중

---

## 1. 현재 설정 상태 평가

### 1.1 Overall Score

| 단계 | 적절성 | 설명 |
|------|--------|------|
| MVP/베타 | ✅ 75% | 출시 가능, 모니터링 주시 필요 |
| 정식 서비스 | ⚠️ 65% | DLQ, Graceful Shutdown 추가 권장 |
| 대규모 트래픽 | ❌ 50% | 서킷 브레이커, 캐시, 분산 추적 필수 |

### 1.2 현재 갖춰진 항목

| 카테고리 | 항목 | 상태 |
|----------|------|------|
| **보안** | Kafka SASL/SSL | ✅ |
| | JWT + OAuth2 인증 | ✅ |
| | Kafka trusted.packages 제한 | ✅ |
| **안정성** | Kafka 멱등성 (enable.idempotence) | ✅ |
| | Kafka isolation.level=read_committed | ✅ |
| | HikariCP 커넥션 풀 설정 | ✅ |
| **운영** | Flyway DB 마이그레이션 | ✅ |
| | Actuator 기본 모니터링 | ✅ |
| | Rate Limiting 의존성 (Bucket4j) | ✅ |
| | local/prod 프로필 분리 | ✅ |

---

## 2. 필수 추가 설정 (Priority: Critical)

### 2.1 Graceful Shutdown

배포 시 진행 중인 요청과 Kafka 메시지 처리를 안전하게 완료하기 위해 필수.

```yaml
# application.yml
server:
  shutdown: graceful

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s
```

**효과:**
- 배포 시 새 요청 거부, 기존 요청 처리 완료 대기
- Kafka Consumer가 현재 처리 중인 메시지 완료 후 종료
- 최대 30초 대기 후 강제 종료

---

### 2.2 Redis 보안 설정 (prod)

```yaml
# application.yml - prod 프로필
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

**Azure Cache for Redis 사용 시 추가 설정:**
```yaml
spring:
  data:
    redis:
      host: ${AZURE_REDIS_HOST}  # xxx.redis.cache.windows.net
      port: 6380  # SSL 포트
      password: ${AZURE_REDIS_ACCESS_KEY}
      ssl:
        enabled: true
```

---

### 2.3 Kafka Consumer Concurrency

파티션 수에 맞는 동시 처리 설정.

```yaml
# application.yml
spring:
  kafka:
    listener:
      concurrency: ${KAFKA_CONSUMER_CONCURRENCY:3}
      ack-mode: record  # 메시지별 ACK (안전)
      # ack-mode: batch  # 배치 ACK (성능)
```

**설정 가이드:**
| 파티션 수 | 권장 concurrency | 비고 |
|-----------|-----------------|------|
| 10 | 3~5 | 초기 서비스 |
| 30 | 10~15 | 성장기 |
| 50+ | 20~30 | 대규모 |

> ⚠️ concurrency > 파티션 수면 일부 Consumer가 놀게 됨

---

### 2.4 Kafka Dead Letter Queue (DLQ)

메시지 처리 실패 시 재시도 후 DLQ로 이동. **코드 레벨 구현 필요.**

**방법 1: @RetryableTopic (Spring Kafka 2.7+)**

```java
@Component
public class ChatKafkaConsumer {

    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 2.0),
        dltTopicSuffix = "-dlt",
        autoCreateTopics = "false"
    )
    @KafkaListener(topics = "chat-messages", groupId = "nonstop-chat")
    public void consume(ChatMessageDto message) {
        // 메시지 처리 로직
        chatService.saveAndBroadcast(message);
    }

    @DltHandler
    public void handleDlt(ChatMessageDto message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        // DLQ 메시지 처리 (로깅, 알림 등)
        log.error("DLT 메시지 수신: topic={}, message={}", topic, message);
        alertService.sendSlackAlert("Kafka DLT 메시지 발생: " + message.getRoomId());
    }
}
```

**방법 2: DefaultErrorHandler (커스텀 제어)**

```java
@Configuration
public class KafkaConsumerConfig {

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate) {

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // DLQ로 보내는 DeadLetterPublishingRecoverer
        DeadLetterPublishingRecoverer recoverer =
            new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + "-dlt", record.partition()));

        // 3번 재시도 후 DLQ로 이동
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
            recoverer,
            new FixedBackOff(1000L, 3L)  // 1초 간격, 3번 재시도
        );

        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
}
```

**DLQ 토픽 생성 (프로덕션 권장):**

```java
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

---

### 2.5 Kafka 토픽 자동 생성 비활성화 (프로덕션)

프로덕션에서는 토픽을 명시적으로 관리.

```yaml
# application.yml - prod 프로필
spring:
  kafka:
    properties:
      allow.auto.create.topics: false
```

---

## 3. 권장 추가 설정 (Priority: High)

### 3.1 구조화 로깅 (JSON)

ELK, Azure Monitor, CloudWatch 등 로그 수집 시스템 연동용.

**build.gradle:**
```gradle
implementation 'net.logstash.logback:logstash-logback-encoder:7.4'
```

**src/main/resources/logback-spring.xml:**
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

**출력 예시:**
```json
{
  "@timestamp": "2025-12-26T10:30:00.000Z",
  "level": "INFO",
  "logger": "com.app.nonstop.domain.chat.ChatService",
  "message": "메시지 저장 완료",
  "app": "nonstop",
  "env": "prod",
  "traceId": "abc123",
  "userId": "12345"
}
```

---

### 3.2 Health Check 상세 설정

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when_authorized
      probes:
        enabled: true  # Kubernetes liveness/readiness 프로브
      group:
        readiness:
          include: db,redis,kafka
        liveness:
          include: ping
  health:
    db:
      enabled: true
    redis:
      enabled: true
    kafka:
      enabled: true
```

**Kubernetes 프로브 예시:**
```yaml
# k8s deployment.yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 28080
  initialDelaySeconds: 30
  periodSeconds: 10

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 28080
  initialDelaySeconds: 10
  periodSeconds: 5
```

---

### 3.3 에러 알림 (Slack)

**의존성:**
```gradle
implementation 'com.slack.api:slack-api-client:1.36.1'
```

**AlertService.java:**
```java
@Service
@Slf4j
public class AlertService {

    @Value("${slack.webhook.url}")
    private String webhookUrl;

    private final Slack slack = Slack.getInstance();

    public void sendSlackAlert(String message) {
        try {
            Payload payload = Payload.builder()
                .text(":rotating_light: *Nonstop Alert*\n" + message)
                .build();
            slack.send(webhookUrl, payload);
        } catch (Exception e) {
            log.error("Slack 알림 전송 실패", e);
        }
    }

    public void sendErrorAlert(String title, Throwable error) {
        String message = String.format(
            "*%s*\n```%s: %s```\n`%s`",
            title,
            error.getClass().getSimpleName(),
            error.getMessage(),
            LocalDateTime.now()
        );
        sendSlackAlert(message);
    }
}
```

**application.yml:**
```yaml
slack:
  webhook:
    url: ${SLACK_WEBHOOK_URL}
```

---

### 3.4 WebSocket 세션 제한

리소스 보호를 위한 동시 연결 제한.

```java
@Configuration
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.setMessageSizeLimit(64 * 1024);      // 64KB 메시지 크기 제한
        registry.setSendBufferSizeLimit(512 * 1024);  // 512KB 버퍼 제한
        registry.setSendTimeLimit(20 * 1000);         // 20초 전송 타임아웃
    }

    // 사용자당 연결 수 제한 (인터셉터)
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new WebSocketSessionLimitInterceptor());
    }
}
```

```java
@Component
public class WebSocketSessionLimitInterceptor implements ChannelInterceptor {

    private static final int MAX_SESSIONS_PER_USER = 5;
    private final Map<Long, Set<String>> userSessions = new ConcurrentHashMap<>();

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            Long userId = extractUserId(accessor);
            String sessionId = accessor.getSessionId();

            userSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet());
            Set<String> sessions = userSessions.get(userId);

            if (sessions.size() >= MAX_SESSIONS_PER_USER) {
                throw new MessageDeliveryException("세션 제한 초과: userId=" + userId);
            }
            sessions.add(sessionId);
        }

        return message;
    }
}
```

---

## 4. 중장기 개선 사항 (Priority: Medium)

### 4.1 서킷 브레이커 (Resilience4j)

외부 API (FCM, Azure Blob 등) 장애 대응.

**build.gradle:**
```gradle
implementation 'io.github.resilience4j:resilience4j-spring-boot3:2.2.0'
```

**application.yml:**
```yaml
resilience4j:
  circuitbreaker:
    instances:
      fcm:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
        permittedNumberOfCallsInHalfOpenState: 5
      azure-blob:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 60s
```

**사용 예시:**
```java
@Service
public class FcmPushService {

    @CircuitBreaker(name = "fcm", fallbackMethod = "sendPushFallback")
    public void sendPush(String token, String title, String body) {
        // FCM 전송 로직
    }

    public void sendPushFallback(String token, String title, String body, Throwable t) {
        log.warn("FCM 서킷 오픈, 푸시 큐에 저장: token={}", token);
        pushRetryQueue.add(new PushRequest(token, title, body));
    }
}
```

---

### 4.2 분산 추적 (Micrometer Tracing)

요청 흐름 추적 (WebSocket → Kafka → DB).

**build.gradle:**
```gradle
implementation 'io.micrometer:micrometer-tracing-bridge-brave'
implementation 'io.zipkin.reporter2:zipkin-reporter-brave'
```

**application.yml:**
```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 프로덕션에서는 0.1~0.5 권장
  zipkin:
    tracing:
      endpoint: ${ZIPKIN_URL:http://localhost:9411/api/v2/spans}
```

---

### 4.3 Spring Cache (Redis)

자주 조회되는 데이터 캐싱.

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .withCacheConfiguration("universities",
                config.entryTtl(Duration.ofHours(24)))
            .withCacheConfiguration("user-profiles",
                config.entryTtl(Duration.ofMinutes(30)))
            .build();
    }
}
```

**사용 예시:**
```java
@Service
public class UniversityService {

    @Cacheable(value = "universities", key = "#id")
    public UniversityDto getUniversity(Long id) {
        return universityMapper.findById(id);
    }

    @CacheEvict(value = "universities", key = "#id")
    public void updateUniversity(Long id, UniversityDto dto) {
        universityMapper.update(id, dto);
    }
}
```

---

## 5. 설정 체크리스트

### 5.1 배포 전 필수 체크

- [ ] Graceful Shutdown 설정
- [ ] Redis 패스워드 설정 (prod)
- [ ] Kafka Consumer Concurrency 설정
- [ ] Kafka DLQ 구현
- [ ] 토픽 자동 생성 비활성화 (prod)
- [ ] 환경변수 모두 설정 확인

### 5.2 정식 서비스 전 권장

- [ ] 구조화 로깅 (JSON)
- [ ] Health Check 상세 설정
- [ ] 에러 알림 (Slack) 연동
- [ ] WebSocket 세션 제한

### 5.3 대규모 트래픽 대응

- [ ] 서킷 브레이커 (Resilience4j)
- [ ] 분산 추적 (Micrometer Tracing)
- [ ] Spring Cache (Redis)
- [ ] Kafka 파티션 수 조정
- [ ] 수평 확장 (Pod 증설)

---

## 6. 환경변수 목록 (전체)

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
REDIS_PASSWORD=           # prod 필수

# Kafka
KAFKA_BOOTSTRAP_SERVERS=
KAFKA_CONNECTION_STRING=
KAFKA_CONSUMER_CONCURRENCY=3

# Azure Blob Storage
AZURE_STORAGE_ACCOUNT_NAME=
AZURE_STORAGE_ACCOUNT_KEY=
AZURE_STORAGE_ENDPOINT=
AZURE_STORAGE_CONTAINER_NAME=

# Auth
JWT_SECRET_KEY=
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=

# Mail
MAIL_USERNAME=
MAIL_PASSWORD=

# Admin
ADMIN_EMAIL=

# Monitoring (선택)
SLACK_WEBHOOK_URL=
ZIPKIN_URL=

# HikariCP (선택, 기본값 있음)
HIKARI_MAX_POOL_SIZE=10
HIKARI_MIN_IDLE=5
```

---

## 7. 참고 자료

- [Spring Kafka Documentation](https://docs.spring.io/spring-kafka/reference/)
- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Micrometer Tracing](https://micrometer.io/docs/tracing)
- [Azure Cache for Redis with Spring](https://learn.microsoft.com/en-us/azure/azure-cache-for-redis/cache-java-get-started)

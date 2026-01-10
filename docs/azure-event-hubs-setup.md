# Azure Event Hubs 연결 설정 가이드

## 개요

이 문서는 Spring Boot 애플리케이션을 Azure Event Hubs(Kafka 호환)에 연결하는 과정을 설명합니다.

## 문제 상황

초기에 Azure Event Hubs 연결 시도 시 다음과 같은 문제가 발생했습니다:

1. **네트워크 연결 실패**: TimeoutException 발생
2. **환경 변수 파싱 오류**: `.env` 파일의 공백 문제
3. **Consumer 보안 설정 미적용**: YAML 설정이 Consumer에 제대로 전파되지 않음
4. **토픽 인식 오류**: Event Hub가 생성되지 않아 UNKNOWN_TOPIC_OR_PARTITION 에러 발생

## 해결 과정

### 1. 환경 변수 설정 수정

`.env` 파일에서 `=` 기호 양쪽의 공백을 제거했습니다.

**수정 전:**
```properties
KAFKA_BOOTSTRAP_SERVERS = nonstop-kaf.servicebus.windows.net:9093
KAFKA_CONNECTION_STRING = Endpoint= sb://nonstop-kaf.servicebus.windows.net/;...
```

**수정 후:**
```properties
KAFKA_BOOTSTRAP_SERVERS=nonstop-kaf.servicebus.windows.net:9093
KAFKA_CONNECTION_STRING=Endpoint=sb://nonstop-kaf.servicebus.windows.net/;SharedAccessKeyName=kafka-access;SharedAccessKey=...
```

### 2. Java 기반 Kafka 설정으로 전환

YAML 설정만으로는 Consumer에 SASL_SSL 설정이 제대로 적용되지 않는 문제가 있어, Java Configuration으로 전환했습니다.

#### KafkaConsumerConfig.java

Profile별로 ConsumerFactory를 분리하여 local과 prod 환경을 구분했습니다.

```java
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${spring.kafka.listener.concurrency:3}")
    private int concurrency;

    @Value("${KAFKA_CONNECTION_STRING:}")
    private String kafkaConnectionString;

    // Local 프로파일용 Consumer Factory (PLAINTEXT)
    @Bean
    @Profile("local")
    public ConsumerFactory<String, Object> localConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.app.nonstop.domain.chat.dto");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        return new DefaultKafkaConsumerFactory<>(props);
    }

    // Production 프로파일용 Consumer Factory (SASL_SSL)
    @Bean
    @Profile("prod")
    public ConsumerFactory<String, Object> prodConsumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "com.app.nonstop.domain.chat.dto");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");

        // Azure Event Hubs 보안 설정
        props.put("security.protocol", "SASL_SSL");
        props.put("sasl.mechanism", "PLAIN");
        props.put("sasl.jaas.config",
                "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                "username=\"$ConnectionString\" " +
                "password=\"" + kafkaConnectionString + "\";");

        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            ConsumerFactory<String, Object> consumerFactory,
            KafkaTemplate<String, Object> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(concurrency);

        // DLQ 설정
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, ex) -> new TopicPartition(record.topic() + "-dlt", record.partition() % 3));
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }
}
```

#### KafkaProducerConfig.java

Producer도 마찬가지로 Profile별로 분리했습니다.

```java
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${KAFKA_CONNECTION_STRING:}")
    private String kafkaConnectionString;

    // Local 프로파일용 Producer Factory (PLAINTEXT)
    @Bean
    @Profile("local")
    public ProducerFactory<String, Object> localProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    // Production 프로파일용 Producer Factory (SASL_SSL)
    @Bean
    @Profile("prod")
    public ProducerFactory<String, Object> prodProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);

        // Azure Event Hubs 보안 설정
        configProps.put("security.protocol", "SASL_SSL");
        configProps.put("sasl.mechanism", "PLAIN");
        configProps.put("sasl.jaas.config",
                "org.apache.kafka.common.security.plain.PlainLoginModule required " +
                "username=\"$ConnectionString\" " +
                "password=\"" + kafkaConnectionString + "\";");

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }
}
```

### 3. Azure Event Hub 생성

Azure Portal 또는 Azure CLI를 통해 다음 Event Hub를 생성했습니다:

| Event Hub 이름 | 파티션 수 | 보존 기간 | 용도 |
|---------------|---------|---------|------|
| chat-messages | 10 | 7일 | 채팅 메시지 |
| chat-messages-dlt | 3 | 7일 | 채팅 메시지 DLQ |
| chat-read-events | 5 | 1일 | 읽음 이벤트 |
| chat-read-events-dlt | 2 | 7일 | 읽음 이벤트 DLQ |

**Azure CLI 예시:**
```bash
# Event Hub 생성 예시
az eventhubs eventhub create \
  --resource-group <resource-group> \
  --namespace-name nonstop-kaf \
  --name chat-messages \
  --partition-count 10 \
  --message-retention 7
```

### 4. 애플리케이션 실행

Production 프로파일로 실행:
```bash
./gradlew bootRun --args='--spring.profiles.active=prod'
```

## 최종 검증 결과

### 연결 상태
- ✅ SASL_SSL 인증 성공: `Successfully logged in`
- ✅ Consumer 그룹 참여: 6개 Consumer 모두 정상 참여
- ✅ 파티션 할당 완료: 모든 파티션 정상 할당

### 파티션 할당 상세

**chat-messages (10 파티션)**
- consumer-nonstop-chat-1: 파티션 0, 1, 2, 3
- consumer-nonstop-chat-2: 파티션 4, 5, 6
- consumer-nonstop-chat-3: 파티션 7, 8, 9

**chat-read-events (5 파티션)**
- consumer-nonstop-chat-read-4: 파티션 0, 1
- consumer-nonstop-chat-read-5: 파티션 2, 3
- consumer-nonstop-chat-read-6: 파티션 4

### 로그 확인

정상 연결 시 다음과 같은 로그를 확인할 수 있습니다:

```
2026-01-10T11:38:03.839+09:00  INFO ... o.a.k.c.s.authenticator.AbstractLogin    : Successfully logged in.
2026-01-10T11:38:04.685+09:00  INFO ... org.apache.kafka.clients.Metadata        : [Consumer clientId=consumer-nonstop-chat-1, groupId=nonstop-chat] Cluster ID: nonstop-kaf.servicebus.windows.net
2026-01-10T11:38:16.473+09:00  INFO ... o.a.k.c.c.internals.ConsumerCoordinator  : [Consumer clientId=consumer-nonstop-chat-1, groupId=nonstop-chat] Successfully joined group with generation Generation{...}
2026-01-10T11:38:16.530+09:00  INFO ... k.c.c.i.ConsumerRebalanceListenerInvoker : [Consumer clientId=consumer-nonstop-chat-1, groupId=nonstop-chat] Adding newly assigned partitions: chat-messages-0, chat-messages-1, chat-messages-2, chat-messages-3
```

## 주요 설정 파일

### application.yml

```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: nonstop-chat
      auto-offset-reset: earliest
    listener:
      concurrency: ${KAFKA_CONSUMER_CONCURRENCY:3}
```

### .env

```properties
KAFKA_BOOTSTRAP_SERVERS=nonstop-kaf.servicebus.windows.net:9093
KAFKA_CONNECTION_STRING=Endpoint=sb://nonstop-kaf.servicebus.windows.net/;SharedAccessKeyName=kafka-access;SharedAccessKey=<your-key>
```

## 문제 해결 팁

### 1. YAML 설정이 적용되지 않을 때

Spring Boot의 YAML 설정은 중첩된 Kafka Consumer 속성에 대해 제대로 병합되지 않을 수 있습니다. 이 경우 Java Configuration으로 전환하는 것이 안전합니다.

### 2. UNKNOWN_TOPIC_OR_PARTITION 에러

Azure Event Hubs는 자동 토픽 생성을 지원하지 않습니다. 반드시 Azure Portal 또는 Azure CLI를 통해 Event Hub를 수동으로 생성해야 합니다.

### 3. 환경 변수 파싱 오류

`.env` 파일에서 `=` 기호 양쪽에 공백이 있으면 안 됩니다.
- ❌ `KAFKA_BOOTSTRAP_SERVERS = value`
- ✅ `KAFKA_BOOTSTRAP_SERVERS=value`

### 4. Connection String 형식

Azure Event Hubs의 Connection String은 다음 형식을 따라야 합니다:
```
Endpoint=sb://<namespace>.servicebus.windows.net/;SharedAccessKeyName=<key-name>;SharedAccessKey=<key>
```

`Endpoint=` 뒤에 공백이 있으면 안 됩니다.

## 참고 자료

- [Azure Event Hubs for Apache Kafka](https://docs.microsoft.com/azure/event-hubs/event-hubs-for-kafka-ecosystem-overview)
- [Spring Kafka Documentation](https://spring.io/projects/spring-kafka)
- [Azure Event Hubs Quotas and Limits](https://docs.microsoft.com/azure/event-hubs/event-hubs-quotas)

## 작성 정보

- 작성일: 2026-01-10
- 애플리케이션: nonstop
- Azure Event Hubs Namespace: nonstop-kaf.servicebus.windows.net

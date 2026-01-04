# Kafka 토픽 명시적 생성 설계 문서

**버전:** v1.0
**작성일:** 2026-01-04
**상태:** Draft

---

## 1. 개요

### 1.1 목적
Kafka 토픽을 명시적으로 관리하여 프로덕션 환경에서의 안정성을 확보하고, Azure Event Hubs 환경에 맞는 설정을 적용한다.

### 1.2 현재 상태

| 항목 | 상태 | 비고 |
|------|------|------|
| KafkaTopicConfig.java | ✅ 있음 | Bean 정의됨 |
| chat-messages 토픽 | ✅ 정의됨 | 파티션 10, 레플리카 3 |
| chat-messages-dlt 토픽 | ✅ 정의됨 | 파티션 3, 레플리카 3 |
| chat-read-events 토픽 | ✅ 정의됨 | 파티션 5, 레플리카 3 |
| Azure Event Hubs 호환성 | ⚠️ 미검증 | replicas 설정 무시될 수 있음 |
| 환경별 분리 | ❌ 없음 | local/prod 동일 설정 |
| Retention 설정 | ❌ 없음 | 기본값 사용 중 |
| 토픽 자동 생성 비활성화 | ❌ 없음 | prod에서 필요 |

### 1.3 목표

1. Azure Event Hubs 환경에 맞는 토픽 설정
2. 환경별(local/prod) 토픽 구성 분리
3. Retention, 파티션 수 등 상세 설정 적용
4. 프로덕션에서 자동 토픽 생성 비활성화

---

## 2. Azure Event Hubs 제약사항

### 2.1 Kafka API 호환성

Azure Event Hubs는 Kafka 프로토콜을 지원하지만, 일부 기능에 제약이 있습니다.

| 기능 | 지원 여부 | 비고 |
|------|----------|------|
| Topic 생성 (Admin API) | ⚠️ 제한적 | Event Hub로 매핑됨 |
| Replication Factor | ❌ 무시됨 | Azure가 자동 관리 |
| Partition 수 변경 | ⚠️ 제한적 | Azure Portal에서 설정 |
| Retention 설정 | ⚠️ 제한적 | Event Hub 수준에서 설정 |
| Compaction | ❌ 미지원 | |

### 2.2 권장 접근법

```
┌─────────────────────────────────────────────────────────────────┐
│                    환경별 토픽 관리 전략                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  [Local/Dev 환경]                                               │
│  ├── Kafka (Docker)                                             │
│  ├── Spring Boot Admin API로 토픽 자동 생성                     │
│  └── KafkaTopicConfig Bean 활용                                 │
│                                                                 │
│  [Prod 환경 - Azure Event Hubs]                                 │
│  ├── Azure Portal / Terraform으로 Event Hub 사전 생성           │
│  ├── Spring Boot에서 토픽 자동 생성 비활성화                     │
│  └── KafkaTopicConfig는 문서화 용도로만 유지                     │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 3. 토픽 설계

### 3.1 토픽 목록

| 토픽명 | 용도 | Key | 파티션 | Retention |
|--------|------|-----|--------|-----------|
| `chat-messages` | 채팅 메시지 | roomId | 10 | 7일 |
| `chat-messages-dlt` | 메시지 처리 실패 | roomId | 3 | 30일 |
| `chat-read-events` | 읽음 이벤트 | userId | 5 | 1일 |
| `chat-read-events-dlt` | 읽음 이벤트 실패 | userId | 2 | 30일 |

### 3.2 파티션 수 산정 근거

```
┌─────────────────────────────────────────────────────────────────┐
│                    파티션 수 산정 공식                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  파티션 수 = max(예상 처리량 / 단일 파티션 처리량, Consumer 수)  │
│                                                                 │
│  [chat-messages]                                                │
│  - 예상 메시지: 1,000 msg/sec (피크)                            │
│  - 단일 파티션 처리량: ~100 msg/sec                             │
│  - Consumer 동시 처리 수: 3~5                                   │
│  - 권장 파티션: 10개                                            │
│                                                                 │
│  [chat-read-events]                                             │
│  - 예상 이벤트: 500 event/sec (피크)                            │
│  - Consumer 동시 처리 수: 3                                     │
│  - 권장 파티션: 5개                                             │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 3.3 Retention 설정 근거

| 토픽 | Retention | 근거 |
|------|-----------|------|
| `chat-messages` | 7일 | 메시지는 DB에 영구 저장, Kafka는 임시 버퍼 |
| `chat-messages-dlt` | 30일 | 실패 메시지 분석 및 재처리 여유 |
| `chat-read-events` | 1일 | 읽음 상태는 최신값만 유효 |
| `chat-read-events-dlt` | 30일 | 실패 이벤트 분석 |

---

## 4. 구현 설계

### 4.1 디렉토리 구조

```
com.app.nonstop.global.config
├── KafkaTopicConfig.java       # 수정: 환경별 분리, 상세 설정
├── KafkaProducerConfig.java    # 기존 유지
└── KafkaConsumerConfig.java    # 기존 유지
```

### 4.2 KafkaTopicConfig.java (수정)

```java
package com.app.nonstop.global.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

import java.time.Duration;

@Configuration
public class KafkaTopicConfig {

    /**
     * 토픽 설정 상수
     * - Azure Event Hubs에서는 replicas 설정이 무시됨
     * - Prod 환경에서는 Azure Portal/Terraform으로 사전 생성 권장
     */
    public static class Topics {
        public static final String CHAT_MESSAGES = "chat-messages";
        public static final String CHAT_MESSAGES_DLT = "chat-messages-dlt";
        public static final String CHAT_READ_EVENTS = "chat-read-events";
        public static final String CHAT_READ_EVENTS_DLT = "chat-read-events-dlt";
    }

    // ========================================
    // Local 환경용 토픽 설정 (Docker Kafka)
    // ========================================

    @Bean
    @Profile("local")
    public NewTopic chatMessagesTopicLocal() {
        return TopicBuilder.name(Topics.CHAT_MESSAGES)
                .partitions(3)  // Local에서는 적은 파티션
                .replicas(1)    // Local에서는 단일 복제
                .config(TopicConfig.RETENTION_MS_CONFIG,
                        String.valueOf(Duration.ofDays(1).toMillis()))
                .build();
    }

    @Bean
    @Profile("local")
    public NewTopic chatMessagesDltTopicLocal() {
        return TopicBuilder.name(Topics.CHAT_MESSAGES_DLT)
                .partitions(1)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG,
                        String.valueOf(Duration.ofDays(7).toMillis()))
                .build();
    }

    @Bean
    @Profile("local")
    public NewTopic chatReadEventsTopicLocal() {
        return TopicBuilder.name(Topics.CHAT_READ_EVENTS)
                .partitions(2)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG,
                        String.valueOf(Duration.ofHours(6).toMillis()))
                .build();
    }

    @Bean
    @Profile("local")
    public NewTopic chatReadEventsDltTopicLocal() {
        return TopicBuilder.name(Topics.CHAT_READ_EVENTS_DLT)
                .partitions(1)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG,
                        String.valueOf(Duration.ofDays(7).toMillis()))
                .build();
    }

    // ========================================
    // Prod 환경용 토픽 설정 (Azure Event Hubs)
    // 주의: Azure에서는 이 Bean으로 토픽이 생성되지 않을 수 있음
    // Azure Portal/Terraform으로 사전 생성 필요
    // ========================================

    @Bean
    @Profile("prod")
    public NewTopic chatMessagesTopicProd() {
        return TopicBuilder.name(Topics.CHAT_MESSAGES)
                .partitions(10)
                .replicas(3)  // Azure에서 무시됨
                .config(TopicConfig.RETENTION_MS_CONFIG,
                        String.valueOf(Duration.ofDays(7).toMillis()))
                .build();
    }

    @Bean
    @Profile("prod")
    public NewTopic chatMessagesDltTopicProd() {
        return TopicBuilder.name(Topics.CHAT_MESSAGES_DLT)
                .partitions(3)
                .replicas(3)
                .config(TopicConfig.RETENTION_MS_CONFIG,
                        String.valueOf(Duration.ofDays(30).toMillis()))
                .build();
    }

    @Bean
    @Profile("prod")
    public NewTopic chatReadEventsTopicProd() {
        return TopicBuilder.name(Topics.CHAT_READ_EVENTS)
                .partitions(5)
                .replicas(3)
                .config(TopicConfig.RETENTION_MS_CONFIG,
                        String.valueOf(Duration.ofDays(1).toMillis()))
                .build();
    }

    @Bean
    @Profile("prod")
    public NewTopic chatReadEventsDltTopicProd() {
        return TopicBuilder.name(Topics.CHAT_READ_EVENTS_DLT)
                .partitions(2)
                .replicas(3)
                .config(TopicConfig.RETENTION_MS_CONFIG,
                        String.valueOf(Duration.ofDays(30).toMillis()))
                .build();
    }
}
```

### 4.3 application.yml 수정

```yaml
# ===================================================================
# PROD PROFILE - Kafka 자동 토픽 생성 비활성화
# ===================================================================
---
spring:
  config:
    activate:
      on-profile: prod
  kafka:
    properties:
      # 자동 토픽 생성 비활성화 (Azure Event Hubs에서 권장)
      allow.auto.create.topics: false
```

### 4.4 토픽 상수 클래스 활용

Producer/Consumer에서 토픽명을 하드코딩하지 않고 상수 사용:

```java
// Before
@KafkaListener(topics = "chat-messages", ...)

// After
@KafkaListener(topics = KafkaTopicConfig.Topics.CHAT_MESSAGES, ...)
```

```java
// Before
kafkaTemplate.send("chat-messages", key, message);

// After
kafkaTemplate.send(KafkaTopicConfig.Topics.CHAT_MESSAGES, key, message);
```

---

## 5. Azure Event Hubs 설정 가이드

### 5.1 Azure Portal에서 Event Hub 생성

Azure Event Hubs에서는 Kafka 토픽이 Event Hub로 매핑됩니다.

```
Event Hubs Namespace (= Kafka Cluster)
├── chat-messages (Event Hub = Kafka Topic)
│   ├── Partition Count: 10
│   └── Message Retention: 7 days
├── chat-messages-dlt
│   ├── Partition Count: 3
│   └── Message Retention: 30 days
├── chat-read-events
│   ├── Partition Count: 5
│   └── Message Retention: 1 day
└── chat-read-events-dlt
    ├── Partition Count: 2
    └── Message Retention: 30 days
```

### 5.2 Terraform 예시 (선택)

```hcl
resource "azurerm_eventhub" "chat_messages" {
  name                = "chat-messages"
  namespace_name      = azurerm_eventhub_namespace.main.name
  resource_group_name = azurerm_resource_group.main.name
  partition_count     = 10
  message_retention   = 7
}

resource "azurerm_eventhub" "chat_messages_dlt" {
  name                = "chat-messages-dlt"
  namespace_name      = azurerm_eventhub_namespace.main.name
  resource_group_name = azurerm_resource_group.main.name
  partition_count     = 3
  message_retention   = 30
}

resource "azurerm_eventhub" "chat_read_events" {
  name                = "chat-read-events"
  namespace_name      = azurerm_eventhub_namespace.main.name
  resource_group_name = azurerm_resource_group.main.name
  partition_count     = 5
  message_retention   = 1
}

resource "azurerm_eventhub" "chat_read_events_dlt" {
  name                = "chat-read-events-dlt"
  namespace_name      = azurerm_eventhub_namespace.main.name
  resource_group_name = azurerm_resource_group.main.name
  partition_count     = 2
  message_retention   = 30
}
```

### 5.3 Azure CLI 예시

```bash
# Event Hub 생성
az eventhubs eventhub create \
  --resource-group nonstop-rg \
  --namespace-name nonstop-eventhubs \
  --name chat-messages \
  --partition-count 10 \
  --message-retention 7

az eventhubs eventhub create \
  --resource-group nonstop-rg \
  --namespace-name nonstop-eventhubs \
  --name chat-messages-dlt \
  --partition-count 3 \
  --message-retention 30

az eventhubs eventhub create \
  --resource-group nonstop-rg \
  --namespace-name nonstop-eventhubs \
  --name chat-read-events \
  --partition-count 5 \
  --message-retention 1

az eventhubs eventhub create \
  --resource-group nonstop-rg \
  --namespace-name nonstop-eventhubs \
  --name chat-read-events-dlt \
  --partition-count 2 \
  --message-retention 30
```

---

## 6. Local 개발 환경 (Docker Kafka)

### 6.1 docker-compose.yml

```yaml
version: '3.8'
services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on:
      - zookeeper
    ports:
      - "9092:9092"
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:9092
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
```

### 6.2 Local 환경 application.yml

```yaml
---
spring:
  config:
    activate:
      on-profile: local
  kafka:
    bootstrap-servers: localhost:9092
    # Local에서는 SASL 불필요
    properties:
      security.protocol: PLAINTEXT
```

---

## 7. 수정 대상 파일

### 7.1 수정 필요

| 파일 | 변경 내용 |
|------|----------|
| `KafkaTopicConfig.java` | Profile 분리, 상수 클래스, Retention 설정 |
| `application.yml` | prod에 `allow.auto.create.topics: false` 추가 |
| `ChatKafkaProducer.java` | 토픽명 상수 사용 |
| `ChatKafkaConsumer.java` | 토픽명 상수 사용 |
| `KafkaConsumerConfig.java` | DLT 토픽명 상수 사용 |

### 7.2 신규 생성

| 파일 | 내용 |
|------|------|
| `ChatReadEventProducer.java` | 읽음 이벤트 발행 (별도 설계 문서) |
| `ChatReadEventConsumer.java` | 읽음 이벤트 소비 (별도 설계 문서) |

---

## 8. 구현 체크리스트

### Phase 1: 코드 수정
- [ ] `KafkaTopicConfig.java` 수정 (Profile 분리, 상수, Retention)
- [ ] `application.yml`에 prod 설정 추가
- [ ] Producer/Consumer에서 토픽명 상수 사용

### Phase 2: Azure 설정
- [ ] Azure Portal에서 Event Hub 생성 (또는 Terraform)
- [ ] 파티션 수, Retention 확인
- [ ] 연결 테스트

### Phase 3: 모니터링
- [ ] Azure Monitor에서 Event Hub 메트릭 확인
- [ ] Consumer lag 알림 설정

---

## 9. 검증 방법

### 9.1 Local 환경

```bash
# Kafka 토픽 목록 확인
docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092

# 토픽 상세 정보
docker exec -it kafka kafka-topics --describe \
  --topic chat-messages \
  --bootstrap-server localhost:9092
```

### 9.2 Azure Event Hubs

```bash
# Azure CLI로 Event Hub 목록 확인
az eventhubs eventhub list \
  --resource-group nonstop-rg \
  --namespace-name nonstop-eventhubs \
  --output table
```

---

## 10. 참고

### 10.1 PRD 요구사항 (섹션 3.7.2)
> - `chat-messages` 토픽: 파티션 수 초기 10~50개 (roomId 키 기반), replication factor 최소 3.
> - `chat-read-events` 토픽: 파티션 수 5~10개 (userId 키 기반), replication factor 최소 3.
> - Retention policy: `chat-messages` 7~30일, `chat-read-events` 1~3일.

### 10.2 Azure Event Hubs 문서
- [Azure Event Hubs for Apache Kafka](https://docs.microsoft.com/azure/event-hubs/event-hubs-for-kafka-ecosystem-overview)
- [Kafka 호환성 제약사항](https://docs.microsoft.com/azure/event-hubs/event-hubs-kafka-faq)

### 10.3 관련 파일
- `KafkaTopicConfig.java` - 현재 토픽 설정
- `KafkaProducerConfig.java` - Producer 설정
- `KafkaConsumerConfig.java` - Consumer 설정
- `application.yml:58-91` - Kafka 설정

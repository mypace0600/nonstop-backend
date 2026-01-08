package com.app.nonstop.global.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.TopicBuilder;

import java.time.Duration;

@Configuration
public class KafkaTopicConfig {

    /**
     * 토픽 설정 상수
     * - Azure Event Hubs에서는 replicas 설정이 무시되지만, 명시적으로 3으로 설정
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
                .partitions(3)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(1).toMillis()))
                .build();
    }

    @Bean
    @Profile("local")
    public NewTopic chatMessagesDltTopicLocal() {
        return TopicBuilder.name(Topics.CHAT_MESSAGES_DLT)
                .partitions(1)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(7).toMillis()))
                .build();
    }

    @Bean
    @Profile("local")
    public NewTopic chatReadEventsTopicLocal() {
        return TopicBuilder.name(Topics.CHAT_READ_EVENTS)
                .partitions(2)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofHours(6).toMillis()))
                .build();
    }

    @Bean
    @Profile("local")
    public NewTopic chatReadEventsDltTopicLocal() {
        return TopicBuilder.name(Topics.CHAT_READ_EVENTS_DLT)
                .partitions(1)
                .replicas(1)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(7).toMillis()))
                .build();
    }

    // ========================================
    // Prod 환경용 토픽 설정 (Azure Event Hubs)
    // ========================================

    @Bean
    @Profile("prod")
    public NewTopic chatMessagesTopicProd() {
        return TopicBuilder.name(Topics.CHAT_MESSAGES)
                .partitions(10)
                .replicas(3)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(7).toMillis()))
                .build();
    }

    @Bean
    @Profile("prod")
    public NewTopic chatMessagesDltTopicProd() {
        return TopicBuilder.name(Topics.CHAT_MESSAGES_DLT)
                .partitions(3)
                .replicas(3)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(30).toMillis()))
                .build();
    }

    @Bean
    @Profile("prod")
    public NewTopic chatReadEventsTopicProd() {
        return TopicBuilder.name(Topics.CHAT_READ_EVENTS)
                .partitions(5)
                .replicas(3)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(1).toMillis()))
                .build();
    }

    @Bean
    @Profile("prod")
    public NewTopic chatReadEventsDltTopicProd() {
        return TopicBuilder.name(Topics.CHAT_READ_EVENTS_DLT)
                .partitions(2)
                .replicas(3)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(30).toMillis()))
                .build();
    }
}

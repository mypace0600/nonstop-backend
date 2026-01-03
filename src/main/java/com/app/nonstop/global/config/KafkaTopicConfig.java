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

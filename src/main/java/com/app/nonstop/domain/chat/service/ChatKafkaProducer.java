package com.app.nonstop.domain.chat.service;

import com.app.nonstop.domain.chat.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendMessage(String topic, ChatMessageDto message) {
        log.info("Sending message to Kafka topic: {}, message: {}", topic, message);
        // roomId를 key로 사용하여 메시지 순서 보장
        this.kafkaTemplate.send(topic, String.valueOf(message.getRoomId()), message);
    }
}

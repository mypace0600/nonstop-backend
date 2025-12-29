package com.app.nonstop.domain.chat.service;

import com.app.nonstop.domain.chat.dto.ChatMessageDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
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
}

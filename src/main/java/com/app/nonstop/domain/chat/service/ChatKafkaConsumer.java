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
        log.info("Consumed message from Kafka: roomId={}, senderId={}", message.getRoomId(), message.getSenderId());
        chatService.saveAndBroadcastMessage(message);
    }

    @DltHandler
    public void handleDlt(ChatMessageDto message, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("DLT message received - Failed to process message: topic={}, roomId={}, senderId={}, content={}",
                topic, message.getRoomId(), message.getSenderId(), message.getContent());
        // TODO: 관리자 알림 전송 (Slack, Email 등)
        // TODO: DB에 실패 로그 저장
    }
}

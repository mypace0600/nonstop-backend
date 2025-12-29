package com.app.nonstop.domain.chat.controller;

import com.app.nonstop.domain.chat.dto.ChatMessageDto;
import com.app.nonstop.domain.chat.service.ChatKafkaProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class WebSocketChatController {

    private final ChatKafkaProducer chatKafkaProducer;

    /**
     * WebSocket "/pub/chat/message"로 들어오는 메시징을 처리합니다.
     */
    @MessageMapping("/chat/message")
    public void message(ChatMessageDto message) {
        // 받은 메시지를 Kafka 토픽으로 전송
        chatKafkaProducer.sendMessage("chat-messages", message);
    }
}

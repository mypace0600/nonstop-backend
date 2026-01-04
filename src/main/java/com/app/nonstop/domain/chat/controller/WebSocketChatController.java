package com.app.nonstop.domain.chat.controller;

import com.app.nonstop.domain.chat.dto.ChatMessageDto;
import com.app.nonstop.domain.chat.service.ChatKafkaProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketChatController {

    private final ChatKafkaProducer chatKafkaProducer;

    /**
     * WebSocket "/pub/chat/message"로 들어오는 메시징을 처리합니다.
     */
    @MessageMapping("/chat/message")
    public void message(ChatMessageDto message, SimpMessageHeaderAccessor headerAccessor) {
        // 세션에서 인증된 userId 가져오기
        Map<String, Object> sessionAttributes = headerAccessor.getSessionAttributes();
        if (sessionAttributes == null || sessionAttributes.get("userId") == null) {
            log.warn("Unauthorized message attempt: no userId in session");
            return;
        }

        Long authenticatedUserId = (Long) sessionAttributes.get("userId");

        // 클라이언트가 보낸 senderId를 무시하고, 인증된 userId로 강제 할당
        message.setSenderId(authenticatedUserId);

        // 받은 메시지를 Kafka 토픽으로 전송
        chatKafkaProducer.sendMessage("chat-messages", message);
    }
}

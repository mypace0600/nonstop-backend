package com.app.nonstop.domain.chat.controller;

import com.app.nonstop.domain.chat.dto.ChatMessageDto;
import com.app.nonstop.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Slf4j
@Controller
@RequiredArgsConstructor
public class WebSocketChatController {

    private final ChatService chatService;

    @MessageMapping("/chat/message")
    public void handleMessage(@Payload ChatMessageDto message) {
        log.info("Received message via WebSocket: roomId={}, senderId={}", message.getRoomId(), message.getSenderId());

        // 메시지 시간 설정
        message.setSentAt(LocalDateTime.now());

        // 직접 메시지 저장 및 브로드캐스트
        chatService.saveAndBroadcastMessage(message);
    }
}

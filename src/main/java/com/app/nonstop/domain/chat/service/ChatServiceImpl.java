package com.app.nonstop.domain.chat.service;

import com.app.nonstop.domain.chat.dto.ChatMessageDto;
import com.app.nonstop.mapper.ChatMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatMapper chatMapper;

    @Override
    @Transactional
    public void saveAndBroadcastMessage(ChatMessageDto message) {
        // 1. 메시지 DB 저장
        message.setSentAt(LocalDateTime.now()); // 서버 시간으로 설정
        chatMapper.insertMessage(message);

        // 2. WebSocket으로 메시지 브로드캐스팅
        messagingTemplate.convertAndSend("/sub/chat/room/" + message.getRoomId(), message);
    }
}

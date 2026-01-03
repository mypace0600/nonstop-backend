package com.app.nonstop.domain.chat.service;

import com.app.nonstop.domain.chat.dto.ChatMessageDto;
import com.app.nonstop.domain.chat.dto.MessageResponseDto;
import com.app.nonstop.global.common.exception.ResourceNotFoundException;
import com.app.nonstop.mapper.ChatMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatMapper chatMapper;

    @Override
    @Transactional
    public void saveAndBroadcastMessage(ChatMessageDto message) {
        // 1. clientMessageId로 중복 체크
        if (message.getClientMessageId() != null
                && chatMapper.existsByClientMessageId(message.getClientMessageId())) {
            log.warn("Duplicate message detected: clientMessageId={}", message.getClientMessageId());
            return;
        }

        // 2. 메시지 DB 저장
        message.setSentAt(LocalDateTime.now());
        chatMapper.insertMessage(message);

        // 3. WebSocket으로 메시지 브로드캐스팅
        messagingTemplate.convertAndSend("/sub/chat/room/" + message.getRoomId(), message);

        log.info("Message saved and broadcasted: messageId={}, roomId={}, clientMessageId={}",
                message.getMessageId(), message.getRoomId(), message.getClientMessageId());
    }

    @Override
    public List<MessageResponseDto> getMessages(Long roomId, Long userId, int limit, int offset) {
        return chatMapper.findMessagesByRoomId(roomId, userId, limit, offset);
    }

    @Override
    @Transactional
    public void deleteMessageForMe(Long roomId, Long userId, Long messageId) {
        // 메시지가 해당 채팅방에 존재하는지 확인
        if (!chatMapper.isMessageInRoom(messageId, roomId)) {
            throw new ResourceNotFoundException("Message not found in this chat room");
        }

        // 메시지 삭제 기록 추가
        chatMapper.insertMessageDeletion(messageId, userId);

        log.info("Message deleted for user: messageId={}, userId={}", messageId, userId);
    }
}

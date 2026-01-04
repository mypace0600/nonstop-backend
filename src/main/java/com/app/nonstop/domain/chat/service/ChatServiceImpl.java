package com.app.nonstop.domain.chat.service;

import com.app.nonstop.domain.chat.dto.ChatMessageDto;
import com.app.nonstop.domain.chat.dto.MessageResponseDto;
import com.app.nonstop.domain.chat.entity.MessageType;
import com.app.nonstop.global.common.exception.ResourceNotFoundException;
import com.app.nonstop.mapper.ChatMapper;
import com.app.nonstop.mapper.ChatRoomMapper;
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

    private static final int MAX_MESSAGE_LENGTH = 5000;

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatMapper chatMapper;
    private final ChatRoomMapper chatRoomMapper;

    @Override
    @Transactional
    public void saveAndBroadcastMessage(ChatMessageDto message) {
        // 1. 메시지 유효성 검증
        if (!validateMessage(message)) {
            return;
        }

        // 2. 발신자가 채팅방 멤버인지 검증
        if (!chatRoomMapper.isMemberOfRoom(message.getRoomId(), message.getSenderId())) {
            log.warn("Unauthorized message attempt: senderId={} is not a member of roomId={}",
                    message.getSenderId(), message.getRoomId());
            return;
        }

        // 3. clientMessageId로 중복 체크
        if (message.getClientMessageId() != null
                && chatMapper.existsByClientMessageId(message.getClientMessageId())) {
            log.warn("Duplicate message detected: clientMessageId={}", message.getClientMessageId());
            return;
        }

        // 4. 메시지 DB 저장
        message.setSentAt(LocalDateTime.now());
        chatMapper.insertMessage(message);

        // 5. WebSocket으로 메시지 브로드캐스팅
        messagingTemplate.convertAndSend("/sub/chat/room/" + message.getRoomId(), message);

        log.info("Message saved and broadcasted: messageId={}, roomId={}, clientMessageId={}",
                message.getMessageId(), message.getRoomId(), message.getClientMessageId());
    }

    private boolean validateMessage(ChatMessageDto message) {
        // roomId 필수
        if (message.getRoomId() == null) {
            log.warn("Message validation failed: roomId is required");
            return false;
        }

        // senderId 필수
        if (message.getSenderId() == null) {
            log.warn("Message validation failed: senderId is required");
            return false;
        }

        // 시스템 메시지가 아닌 경우 content 필수
        if (!isSystemMessage(message.getType())) {
            if (message.getContent() == null || message.getContent().isBlank()) {
                log.warn("Message validation failed: content is required for non-system messages");
                return false;
            }

            // 메시지 길이 제한
            if (message.getContent().length() > MAX_MESSAGE_LENGTH) {
                log.warn("Message validation failed: content exceeds max length of {}", MAX_MESSAGE_LENGTH);
                return false;
            }
        }

        return true;
    }

    private boolean isSystemMessage(MessageType type) {
        return type != null && (
                type == MessageType.SYSTEM_INVITE ||
                type == MessageType.SYSTEM_LEAVE ||
                type == MessageType.SYSTEM_KICK
        );
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

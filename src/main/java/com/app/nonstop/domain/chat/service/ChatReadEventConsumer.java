package com.app.nonstop.domain.chat.service;

import com.app.nonstop.domain.chat.dto.ChatReadEventDto;
import com.app.nonstop.domain.chat.dto.ChatReadStatusDto;
import com.app.nonstop.mapper.ChatRoomMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatReadEventConsumer {

    private final ChatRoomMapper chatRoomMapper;
    private final SimpMessageSendingOperations messagingTemplate;

    @KafkaListener(
        topics = "chat-read-events",
        groupId = "nonstop-chat-read",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ChatReadEventDto event) {
        log.info("Consumed read event: roomId={}, userId={}, messageId={}",
                event.getRoomId(), event.getUserId(), event.getMessageId());

        try {
            // 1. DB 업데이트 (last_read_message_id) - 멱등성 보장
            chatRoomMapper.updateLastReadMessageIdIfGreater(
                event.getRoomId(),
                event.getUserId(),
                event.getMessageId()
            );

            // 2. WebSocket으로 읽음 상태 브로드캐스트
            broadcastReadStatus(event);

            log.info("Read event processed successfully: roomId={}, userId={}",
                    event.getRoomId(), event.getUserId());

        } catch (Exception e) {
            log.error("Failed to process read event: roomId={}, userId={}, error={}",
                    event.getRoomId(), event.getUserId(), e.getMessage());
            throw e; // DLT로 이동
        }
    }

    private void broadcastReadStatus(ChatReadEventDto event) {
        ChatReadStatusDto status = ChatReadStatusDto.builder()
                .roomId(event.getRoomId())
                .userId(event.getUserId())
                .lastReadMessageId(event.getMessageId())
                .readAt(event.getTimestamp())
                .build();

        // 해당 채팅방의 다른 참여자들에게 읽음 상태 알림
        messagingTemplate.convertAndSend(
            "/sub/chat/room/" + event.getRoomId() + "/read",
            status
        );
    }

    @DltHandler
    public void handleDlt(ChatReadEventDto event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("DLT - Failed to process read event: topic={}, roomId={}, userId={}",
                topic, event.getRoomId(), event.getUserId());
    }
}

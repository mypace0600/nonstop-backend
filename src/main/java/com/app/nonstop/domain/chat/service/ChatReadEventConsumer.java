package com.app.nonstop.domain.chat.service;

import com.app.nonstop.domain.chat.dto.ChatReadEventDto;
import com.app.nonstop.domain.chat.dto.ChatReadStatusDto;
import com.app.nonstop.global.config.KafkaTopicConfig;
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
        topics = KafkaTopicConfig.Topics.CHAT_READ_EVENTS,
        groupId = "${spring.kafka.consumer.group-id}-read",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(ChatReadEventDto event) {
        log.info("Consumed read event: roomId={}, userId={}, messageId={}",
                event.getRoomId(), event.getUserId(), event.getMessageId());

        try {
            // 1. DB 업데이트 (last_read_message_id)
            chatRoomMapper.updateLastReadMessageId(
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
            throw e; // 재시도 및 DLT 처리를 위해 예외 다시 던짐
        }
    }

    private void broadcastReadStatus(ChatReadEventDto event) {
        ChatReadStatusDto status = ChatReadStatusDto.builder()
                .roomId(event.getRoomId())
                .userId(event.getUserId())
                .lastReadMessageId(event.getMessageId())
                .readAt(event.getTimestamp())
                .build();

        // 해당 채팅방의 구독자들에게 읽음 상태 알림
        // 클라이언트는 이 메시지를 받아서 UI(예: 1 제거)를 업데이트
        messagingTemplate.convertAndSend(
            "/sub/chat/room/" + event.getRoomId() + "/read",
            status
        );
    }

    @DltHandler
    public void handleDlt(ChatReadEventDto event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        log.error("DLT - Failed to process read event: topic={}, roomId={}, userId={}",
                topic, event.getRoomId(), event.getUserId());
        // 추후 실패 이벤트에 대한 복구 로직이나 알림 추가 가능
    }
}

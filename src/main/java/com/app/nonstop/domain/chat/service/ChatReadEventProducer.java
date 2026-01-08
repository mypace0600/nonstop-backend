package com.app.nonstop.domain.chat.service;

import com.app.nonstop.domain.chat.dto.ChatReadEventDto;
import com.app.nonstop.global.config.KafkaTopicConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatReadEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendReadEvent(ChatReadEventDto event) {
        log.info("Sending read event to Kafka: roomId={}, userId={}, messageId={}",
                event.getRoomId(), event.getUserId(), event.getMessageId());

        // userId를 key로 사용하여 동일 사용자의 읽음 이벤트 순서 보장
        kafkaTemplate.send(KafkaTopicConfig.Topics.CHAT_READ_EVENTS, String.valueOf(event.getUserId()), event);
    }
}

import com.app.nonstop.domain.chat.dto.ChatMessageDto;
import com.app.nonstop.domain.chat.service.ChatKafkaProducer;
import com.app.nonstop.global.config.KafkaTopicConfig;
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

    private final ChatKafkaProducer chatKafkaProducer;

    @MessageMapping("/chat/message")
    public void handleMessage(@Payload ChatMessageDto message) {
        log.info("Received message via WebSocket: roomId={}, senderId={}", message.getRoomId(), message.getSenderId());

        // 메시지 시간 설정
        message.setSentAt(LocalDateTime.now());

        // Kafka로 메시지 전송
        chatKafkaProducer.sendMessage(KafkaTopicConfig.Topics.CHAT_MESSAGES, message);
    }
}

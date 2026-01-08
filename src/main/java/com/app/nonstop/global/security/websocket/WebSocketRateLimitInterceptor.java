package com.app.nonstop.global.security.websocket;

import com.app.nonstop.global.config.WebSocketProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketRateLimitInterceptor implements ChannelInterceptor {

    private static final String RATE_LIMIT_KEY_PREFIX = "ws:ratelimit:user:";

    private final StringRedisTemplate redisTemplate;
    private final WebSocketProperties properties;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        if (!properties.getRateLimit().isEnabled()) {
            return message;
        }

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        // SEND 명령만 체크
        if (StompCommand.SEND.equals(accessor.getCommand())) {
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes == null) return message;

            Long userId = (Long) sessionAttributes.get("userId");
            if (userId == null) return message;

            if (!checkRateLimit(userId)) {
                log.warn("Rate limit exceeded for user: {}", userId);
                // 메시지 드롭 (null 반환)
                throw new MessageDeliveryException(
                    "Rate limit exceeded. Max " +
                    properties.getRateLimit().getMaxMessagesPerMinute() +
                    " messages per minute.");
            }
        }

        return message;
    }

    private boolean checkRateLimit(Long userId) {
        String key = RATE_LIMIT_KEY_PREFIX + userId;
        int maxMessages = properties.getRateLimit().getMaxMessagesPerMinute();

        Long currentCount = redisTemplate.opsForValue().increment(key);

        if (currentCount != null && currentCount == 1) {
            // 첫 메시지면 1분 TTL 설정
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }

        return currentCount != null && currentCount <= maxMessages;
    }
}

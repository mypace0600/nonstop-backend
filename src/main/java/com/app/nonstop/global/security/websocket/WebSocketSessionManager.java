package com.app.nonstop.global.security.websocket;

import com.app.nonstop.global.config.WebSocketProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSessionManager {

    private static final String SESSION_KEY_PREFIX = "ws:session:user:";
    private static final String SESSION_INFO_PREFIX = "ws:session:info:";

    private final StringRedisTemplate redisTemplate;
    private final WebSocketProperties properties;
    private final SimpMessageSendingOperations messagingTemplate;

    /**
     * 새 세션 등록
     * @return 종료해야 할 기존 세션 ID (없으면 null)
     */
    public String registerSession(Long userId, String sessionId) {
        String userKey = SESSION_KEY_PREFIX + userId;
        String sessionInfoKey = SESSION_INFO_PREFIX + sessionId;
        long now = System.currentTimeMillis();

        // 현재 세션 목록 조회
        Set<String> existingSessions = redisTemplate.opsForZSet().range(userKey, 0, -1);
        String sessionToClose = null;

        if (existingSessions != null &&
            existingSessions.size() >= properties.getSession().getMaxSessionsPerUser()) {
            // 가장 오래된 세션 가져오기 (score가 가장 낮은 것)
            Set<String> oldest = redisTemplate.opsForZSet().range(userKey, 0, 0);
            if (oldest != null && !oldest.isEmpty()) {
                sessionToClose = oldest.iterator().next();
                // 오래된 세션 제거
                redisTemplate.opsForZSet().remove(userKey, sessionToClose);
                redisTemplate.delete(SESSION_INFO_PREFIX + sessionToClose);
                log.info("Removing oldest session for user {}: {}", userId, sessionToClose);
            }
        }

        // 새 세션 등록 (score = timestamp)
        redisTemplate.opsForZSet().add(userKey, sessionId, now);

        // 세션 정보 저장
        Map<String, String> sessionInfo = new HashMap<>();
        sessionInfo.put("userId", String.valueOf(userId));
        sessionInfo.put("connectedAt", String.valueOf(now));
        redisTemplate.opsForHash().putAll(sessionInfoKey, sessionInfo);

        // TTL 설정 (유휴 타임아웃 + 여유)
        int ttlMinutes = properties.getSession().getIdleTimeoutMinutes() + 5;
        redisTemplate.expire(userKey, Duration.ofMinutes(ttlMinutes));
        redisTemplate.expire(sessionInfoKey, Duration.ofMinutes(ttlMinutes));

        log.info("Session registered: userId={}, sessionId={}, totalSessions={}",
                userId, sessionId,
                redisTemplate.opsForZSet().size(userKey));

        return sessionToClose;
    }

    /**
     * 세션 제거
     */
    public void removeSession(Long userId, String sessionId) {
        String userKey = SESSION_KEY_PREFIX + userId;
        String sessionInfoKey = SESSION_INFO_PREFIX + sessionId;

        redisTemplate.opsForZSet().remove(userKey, sessionId);
        redisTemplate.delete(sessionInfoKey);

        log.info("Session removed: userId={}, sessionId={}", userId, sessionId);
    }

    /**
     * 사용자의 현재 세션 수 조회
     */
    public long getSessionCount(Long userId) {
        String userKey = SESSION_KEY_PREFIX + userId;
        Long count = redisTemplate.opsForZSet().size(userKey);
        return count != null ? count : 0;
    }

    /**
     * 특정 세션에 종료 알림 전송
     */
    public void notifySessionClosure(String sessionId, String reason) {
        // 세션 종료 알림 메시지 전송
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", "SESSION_CLOSED");
        payload.put("reason", reason);
        payload.put("timestamp", System.currentTimeMillis());

        messagingTemplate.convertAndSendToUser(
            sessionId,
            "/queue/session",
            payload
        );
    }
}

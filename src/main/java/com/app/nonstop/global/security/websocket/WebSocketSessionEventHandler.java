package com.app.nonstop.global.security.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketSessionEventHandler {

    private final WebSocketSessionManager sessionManager;

    @EventListener
    public void handleSessionConnected(SessionConnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

        if (sessionAttributes == null) return;

        Long userId = (Long) sessionAttributes.get("userId");
        String sessionId = accessor.getSessionId();

        if (userId != null && sessionId != null) {
            // 세션 등록 및 초과 세션 처리
            String oldSessionId = sessionManager.registerSession(userId, sessionId);

            if (oldSessionId != null) {
                // 기존 세션에 종료 알림
                sessionManager.notifySessionClosure(oldSessionId,
                    "Maximum session limit exceeded. New session connected.");
            }
        }
    }

    @EventListener
    public void handleSessionDisconnected(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

        if (sessionAttributes == null) return;

        Long userId = (Long) sessionAttributes.get("userId");
        String sessionId = accessor.getSessionId();

        if (userId != null && sessionId != null) {
            sessionManager.removeSession(userId, sessionId);
        }
    }
}

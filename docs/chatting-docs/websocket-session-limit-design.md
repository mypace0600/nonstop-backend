# WebSocket 세션 제한 설계 문서

**버전:** v1.0
**작성일:** 2026-01-04
**상태:** Draft

---

## 1. 개요

### 1.1 목적
WebSocket 연결에 대한 세션 제한을 적용하여 서버 리소스를 보호하고, 악의적인 클라이언트로부터 시스템을 보호한다.

### 1.2 현재 상태

| 항목 | 상태 | 비고 |
|------|------|------|
| 사용자당 세션 수 제한 | ❌ 없음 | 무제한 연결 가능 |
| 메시지 크기 제한 | ❌ 없음 | 대용량 메시지 가능 |
| 메시지 버퍼 제한 | ❌ 없음 | 기본값 사용 |
| 연결 타임아웃 | ❌ 없음 | 기본값 사용 |
| 하트비트 설정 | ❌ 없음 | 기본값 사용 |
| Rate Limiting | ❌ 없음 | 무제한 메시지 가능 |

### 1.3 목표

1. **리소스 보호**: 사용자당 동시 세션 수 제한
2. **메모리 보호**: 메시지 크기 및 버퍼 제한
3. **연결 관리**: 타임아웃 및 하트비트 설정
4. **어뷰징 방지**: 메시지 전송 Rate Limiting

---

## 2. 세션 제한 정책

### 2.1 제한 항목 설계

```
┌─────────────────────────────────────────────────────────────────┐
│                    WebSocket 세션 제한 정책                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  [사용자당 세션 제한]                                            │
│  ├── 최대 동시 세션: 3개 (모바일 + 웹 + 태블릿)                  │
│  ├── 초과 시: 가장 오래된 세션 종료 (LIFO)                       │
│  └── 저장소: Redis (분산 환경 지원)                              │
│                                                                 │
│  [메시지 제한]                                                   │
│  ├── 최대 메시지 크기: 64KB                                     │
│  ├── 메시지 버퍼: 512KB                                         │
│  └── Rate Limit: 60 msg/min per user                           │
│                                                                 │
│  [연결 제한]                                                     │
│  ├── 핸드셰이크 타임아웃: 10초                                  │
│  ├── 연결 유휴 타임아웃: 10분                                   │
│  ├── 하트비트 간격: 25초                                        │
│  └── 하트비트 타임아웃: 60초                                    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 2.2 제한값 근거

| 항목 | 값 | 근거 |
|------|-----|------|
| 최대 세션 수 | 3 | 일반적 사용 패턴 (폰, 태블릿, PC) |
| 메시지 크기 | 64KB | 채팅 메시지 + 이미지 URL 충분 |
| 버퍼 크기 | 512KB | 8개 메시지 버퍼링 가능 |
| Rate Limit | 60/min | 초당 1개 메시지 (스팸 방지) |
| 하트비트 | 25초 | NAT/방화벽 타임아웃 대응 |

---

## 3. 아키텍처

### 3.1 전체 구조

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           WebSocket 세션 관리 구조                           │
└─────────────────────────────────────────────────────────────────────────────┘

[Client]
    │
    │ WebSocket Connect (/ws/v1/chat?token=xxx)
    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  WebSocketAuthInterceptor (기존)                                            │
│  ├── JWT 토큰 검증                                                          │
│  └── userId 추출 → session attributes 저장                                  │
└─────────────────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  WebSocketSessionLimitInterceptor (신규)                                    │
│  ├── Redis에서 사용자 세션 수 조회                                          │
│  ├── 최대 세션 수 초과 시:                                                  │
│  │   ├── 가장 오래된 세션 종료 (disconnect 이벤트 발송)                     │
│  │   └── 새 세션 허용                                                       │
│  └── Redis에 새 세션 등록                                                   │
└─────────────────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  WebSocketConfig (수정)                                                     │
│  ├── 메시지 크기 제한                                                       │
│  ├── 버퍼 크기 제한                                                         │
│  ├── 하트비트 설정                                                          │
│  └── 타임아웃 설정                                                          │
└─────────────────────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│  WebSocketRateLimitInterceptor (신규)                                       │
│  ├── STOMP SEND 메시지 인터셉트                                             │
│  ├── Redis로 사용자별 메시지 카운트                                         │
│  └── Rate Limit 초과 시 메시지 드롭 + 경고                                  │
└─────────────────────────────────────────────────────────────────────────────┘
    │
    ▼
[WebSocketChatController]
```

### 3.2 컴포넌트 구조

```
com.app.nonstop.global
├── config
│   └── WebSocketConfig.java                    # 수정: 메시지/버퍼/하트비트 설정
├── security
│   └── websocket
│       ├── WebSocketAuthInterceptor.java       # 기존
│       ├── WebSocketSessionManager.java        # 신규: 세션 관리
│       ├── WebSocketSessionLimitHandler.java   # 신규: 연결/종료 이벤트
│       └── WebSocketRateLimitInterceptor.java  # 신규: Rate Limiting
└── properties
    └── WebSocketProperties.java                # 신규: 설정값 관리
```

---

## 4. 상세 설계

### 4.1 설정값 클래스

#### WebSocketProperties.java (신규)
```java
@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "websocket")
public class WebSocketProperties {

    private Session session = new Session();
    private Message message = new Message();
    private Heartbeat heartbeat = new Heartbeat();
    private RateLimit rateLimit = new RateLimit();

    @Getter
    @Setter
    public static class Session {
        private int maxSessionsPerUser = 3;
        private int handshakeTimeoutSeconds = 10;
        private int idleTimeoutMinutes = 10;
    }

    @Getter
    @Setter
    public static class Message {
        private int maxSizeKb = 64;
        private int bufferSizeKb = 512;
        private int sendBufferSizeKb = 512;
    }

    @Getter
    @Setter
    public static class Heartbeat {
        private int intervalSeconds = 25;
        private int timeoutSeconds = 60;
    }

    @Getter
    @Setter
    public static class RateLimit {
        private int maxMessagesPerMinute = 60;
        private boolean enabled = true;
    }
}
```

#### application.yml 설정
```yaml
websocket:
  session:
    max-sessions-per-user: 3
    handshake-timeout-seconds: 10
    idle-timeout-minutes: 10
  message:
    max-size-kb: 64
    buffer-size-kb: 512
    send-buffer-size-kb: 512
  heartbeat:
    interval-seconds: 25
    timeout-seconds: 60
  rate-limit:
    max-messages-per-minute: 60
    enabled: true
```

### 4.2 세션 관리자

#### WebSocketSessionManager.java (신규)
```java
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
```

### 4.3 세션 이벤트 핸들러

#### WebSocketSessionEventHandler.java (신규)
```java
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
```

### 4.4 Rate Limiting 인터셉터

#### WebSocketRateLimitInterceptor.java (신규)
```java
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

        if (currentCount == 1) {
            // 첫 메시지면 1분 TTL 설정
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }

        return currentCount <= maxMessages;
    }
}
```

### 4.5 WebSocketConfig 수정

```java
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;
    private final WebSocketRateLimitInterceptor rateLimitInterceptor;
    private final WebSocketProperties properties;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/sub", "/queue")
                .setHeartbeatValue(new long[]{
                    properties.getHeartbeat().getIntervalSeconds() * 1000L,
                    properties.getHeartbeat().getIntervalSeconds() * 1000L
                })
                .setTaskScheduler(heartbeatScheduler());

        registry.setApplicationDestinationPrefixes("/pub");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/v1/chat")
                .addInterceptors(webSocketAuthInterceptor)
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setStreamBytesLimit(properties.getMessage().getMaxSizeKb() * 1024)
                .setHttpMessageCacheSize(1000)
                .setDisconnectDelay(30 * 1000L);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
        registry.setMessageSizeLimit(properties.getMessage().getMaxSizeKb() * 1024)
                .setSendBufferSizeLimit(properties.getMessage().getSendBufferSizeKb() * 1024)
                .setSendTimeLimit(20 * 1000)
                .setTimeToFirstMessage(30 * 1000);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(rateLimitInterceptor);
    }

    @Bean
    public TaskScheduler heartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }
}
```

---

## 5. 시퀀스 다이어그램

### 5.1 세션 제한 동작

```
┌────────┐       ┌────────────────────┐       ┌───────────────────┐       ┌───────┐
│ Client │       │WebSocketAuthInterceptor│   │WebSocketSessionManager│   │ Redis │
│ (New)  │       └──────────┬─────────┘       └─────────┬─────────┘       └───┬───┘
└───┬────┘                  │                           │                     │
    │                       │                           │                     │
    │  Connect              │                           │                     │
    │──────────────────────>│                           │                     │
    │                       │                           │                     │
    │                       │  JWT 검증                  │                     │
    │                       │─────────────────────────> │                     │
    │                       │                           │                     │
    │                       │                           │  세션 수 조회        │
    │                       │                           │───────────────────>│
    │                       │                           │                     │
    │                       │                           │  count = 3 (MAX)   │
    │                       │                           │<───────────────────│
    │                       │                           │                     │
    │                       │                           │  가장 오래된 세션 조회 │
    │                       │                           │───────────────────>│
    │                       │                           │                     │
    │                       │                           │  oldSessionId      │
    │                       │                           │<───────────────────│
    │                       │                           │                     │
    │                       │                           │  오래된 세션 삭제    │
    │                       │                           │───────────────────>│
    │                       │                           │                     │
    │                       │                           │  새 세션 등록       │
    │                       │                           │───────────────────>│
    │                       │                           │                     │
    │                       │<──────────────────────────│                     │
    │                       │                           │                     │
    │  Connected            │                           │                     │
    │<──────────────────────│                           │                     │
    │                       │                           │                     │
```

### 5.2 Rate Limit 동작

```
┌────────┐       ┌────────────────────────┐       ┌───────┐
│ Client │       │WebSocketRateLimitInterceptor│   │ Redis │
└───┬────┘       └────────────┬───────────┘       └───┬───┘
    │                         │                       │
    │  SEND (61번째/분)       │                       │
    │────────────────────────>│                       │
    │                         │                       │
    │                         │  INCR 카운트           │
    │                         │──────────────────────>│
    │                         │                       │
    │                         │  count = 61           │
    │                         │<──────────────────────│
    │                         │                       │
    │                         │  61 > 60 (MAX)        │
    │                         │                       │
    │  ERROR: Rate Limit      │                       │
    │<────────────────────────│                       │
    │                         │                       │
```

---

## 6. 에러 처리

### 6.1 세션 제한 초과
```json
{
  "type": "SESSION_CLOSED",
  "reason": "Maximum session limit exceeded. New session connected.",
  "timestamp": 1704355200000
}
```

### 6.2 Rate Limit 초과
```json
{
  "type": "ERROR",
  "code": "RATE_LIMIT_EXCEEDED",
  "message": "Rate limit exceeded. Max 60 messages per minute.",
  "retryAfter": 45
}
```

### 6.3 메시지 크기 초과
- WebSocket 레벨에서 자동 연결 종료
- 클라이언트는 1009 (Message Too Big) 에러 수신

---

## 7. Redis 데이터 구조

### 7.1 세션 관리

```redis
# 사용자별 세션 목록 (Sorted Set, score = 연결 시간)
ws:session:user:{userId}
├── sessionId1 (score: 1704355100000)
├── sessionId2 (score: 1704355200000)
└── sessionId3 (score: 1704355300000)

# 세션 상세 정보 (Hash)
ws:session:info:{sessionId}
├── userId: "123"
└── connectedAt: "1704355200000"
```

### 7.2 Rate Limit

```redis
# 사용자별 분당 메시지 카운트 (String, TTL 60초)
ws:ratelimit:user:{userId} = "45"
```

---

## 8. 모니터링

### 8.1 주요 메트릭

| 메트릭 | 설명 | 알림 임계값 |
|--------|------|------------|
| `websocket.sessions.active` | 현재 활성 세션 수 | > 10,000 |
| `websocket.sessions.per_user.max` | 사용자당 최대 세션 | > 3 (비정상) |
| `websocket.ratelimit.exceeded` | Rate Limit 초과 횟수 | > 100/min |
| `websocket.messages.size.avg` | 평균 메시지 크기 | > 32KB |

### 8.2 로그 포맷

```
INFO  - Session registered: userId={}, sessionId={}, totalSessions={}
INFO  - Session removed: userId={}, sessionId={}
INFO  - Removing oldest session for user {}: {}
WARN  - Rate limit exceeded for user: {}
ERROR - WebSocket connection failed: {}
```

---

## 9. 환경별 설정

### 9.1 Local

```yaml
websocket:
  session:
    max-sessions-per-user: 5    # 개발 시 여유있게
  rate-limit:
    enabled: false              # 개발 시 비활성화
```

### 9.2 Prod

```yaml
websocket:
  session:
    max-sessions-per-user: 3
    idle-timeout-minutes: 10
  message:
    max-size-kb: 64
  rate-limit:
    enabled: true
    max-messages-per-minute: 60
```

---

## 10. 구현 체크리스트

### Phase 1: 기본 설정
- [ ] `WebSocketProperties.java` 생성
- [ ] `application.yml`에 설정 추가
- [ ] `WebSocketConfig.java` 수정 (메시지 크기, 버퍼, 하트비트)

### Phase 2: 세션 관리
- [ ] `WebSocketSessionManager.java` 생성
- [ ] `WebSocketSessionEventHandler.java` 생성
- [ ] Redis 연동 테스트

### Phase 3: Rate Limiting
- [ ] `WebSocketRateLimitInterceptor.java` 생성
- [ ] `WebSocketConfig`에 인터셉터 등록
- [ ] Rate Limit 테스트

### Phase 4: 모니터링
- [ ] Micrometer 메트릭 추가
- [ ] Grafana 대시보드 구성
- [ ] 알림 설정

---

## 11. 클라이언트 가이드

### 11.1 세션 종료 이벤트 처리

```javascript
// 세션 종료 알림 구독
stompClient.subscribe('/user/queue/session', (message) => {
    const event = JSON.parse(message.body);
    if (event.type === 'SESSION_CLOSED') {
        alert('다른 기기에서 로그인하여 현재 세션이 종료되었습니다.');
        // 재연결 시도 또는 로그인 페이지로 이동
    }
});
```

### 11.2 Rate Limit 에러 처리

```javascript
stompClient.onStompError = (frame) => {
    if (frame.body.includes('Rate limit exceeded')) {
        // 메시지 전송 일시 중지
        showWarning('메시지를 너무 빠르게 보내고 있습니다. 잠시 후 다시 시도해주세요.');
    }
};
```

---

## 12. 참고

### 12.1 관련 파일
- `WebSocketConfig.java` - 현재 WebSocket 설정
- `WebSocketAuthInterceptor.java` - 현재 인증 인터셉터

### 12.2 Spring WebSocket 문서
- [WebSocket Transport Configuration](https://docs.spring.io/spring-framework/reference/web/websocket/stomp/configuration.html)
- [STOMP Message Handling](https://docs.spring.io/spring-framework/reference/web/websocket/stomp/handle-simple-broker.html)

### 12.3 PRD 요구사항 (리뷰 문서)
> WebSocket 세션 제한: 메시지 크기, 버퍼, 타임아웃 등 세부 설정 추가

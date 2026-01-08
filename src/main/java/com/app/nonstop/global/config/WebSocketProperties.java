package com.app.nonstop.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

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

package com.app.nonstop.global.config;

import com.app.nonstop.global.security.websocket.WebSocketAuthInterceptor;
import com.app.nonstop.global.security.websocket.WebSocketRateLimitInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

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
                .setAllowedOriginPatterns("*");
        
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

package com.app.nonstop.domain.notification.dto;

import com.app.nonstop.domain.notification.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

public class NotificationDto {

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long actorId;
        private String actorNickname;
        private NotificationType type;
        private Long postId;
        private Long commentId;
        private Long chatRoomId;
        private String message;
        private Boolean isRead;
        private LocalDateTime createdAt;
    }
}

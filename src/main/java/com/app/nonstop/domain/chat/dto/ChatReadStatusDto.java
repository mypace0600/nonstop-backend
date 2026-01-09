package com.app.nonstop.domain.chat.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ChatReadStatusDto {
    private Long roomId;
    private Long userId;
    private Long lastReadMessageId;
    private LocalDateTime readAt;
}

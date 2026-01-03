package com.app.nonstop.domain.chat.dto;

import com.app.nonstop.domain.chat.entity.MessageType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class MessageResponseDto {
    private Long messageId;
    private Long senderId;
    private String senderNickname;
    private String senderProfileImageUrl;
    private String content;
    private MessageType type;
    private LocalDateTime sentAt;
}

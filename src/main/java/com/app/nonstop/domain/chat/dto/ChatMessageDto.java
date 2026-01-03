package com.app.nonstop.domain.chat.dto;

import com.app.nonstop.domain.chat.entity.MessageType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class ChatMessageDto {

    private Long messageId;
    private Long roomId;
    private Long senderId;
    private Long clientMessageId;
    private String senderNickname;
    private String content;
    private MessageType type;
    private LocalDateTime sentAt;

}

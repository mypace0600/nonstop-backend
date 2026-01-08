package com.app.nonstop.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatReadEventDto {
    private Long roomId;
    private Long userId;
    private Long messageId;
    private LocalDateTime timestamp;
}

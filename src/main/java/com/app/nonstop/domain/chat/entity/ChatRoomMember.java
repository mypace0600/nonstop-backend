package com.app.nonstop.domain.chat.entity;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomMember {
    private Long id;
    private Long roomId;
    private Long userId;
    private Long lastReadMessageId;
    private LocalDateTime joinedAt;
    private LocalDateTime leftAt;
}

package com.app.nonstop.domain.chat.entity;

import com.app.nonstop.global.common.entity.BaseTimeEntity;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom extends BaseTimeEntity {
    private Long id;
    private ChatRoomType type;
    private String name;
    private Long creatorId;
    // BaseTimeEntity handles created_at and updated_at
}

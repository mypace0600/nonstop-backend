package com.app.nonstop.domain.chat.entity;

import com.app.nonstop.global.common.entity.BaseTimeEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom extends BaseTimeEntity {
    private Long id;
    private ChatRoomType type;
    private String name;
    private Long creatorId;
    // BaseTimeEntity handles created_at and updated_at
}

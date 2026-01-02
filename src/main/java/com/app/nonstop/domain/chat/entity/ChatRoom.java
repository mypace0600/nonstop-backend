package com.app.nonstop.domain.chat.entity;

import com.app.nonstop.global.common.entity.BaseTimeEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ChatRoom extends BaseTimeEntity {

    private Long id;
    private ChatRoomType type;
    private String name;
    private Long creatorId;
}

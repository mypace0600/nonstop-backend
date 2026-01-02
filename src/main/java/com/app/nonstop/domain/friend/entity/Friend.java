package com.app.nonstop.domain.friend.entity;

import com.app.nonstop.global.common.entity.BaseTimeEntity;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@EqualsAndHashCode(callSuper = false)
public class Friend extends BaseTimeEntity {

    private final Long id;
    private final Long senderId;
    private final Long receiverId;
    private final FriendStatus status;
    private final LocalDateTime deletedAt;

    @Builder
    public Friend(Long id, Long senderId, Long receiverId, FriendStatus status, LocalDateTime deletedAt) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.status = status;
        this.deletedAt = deletedAt;
    }
}

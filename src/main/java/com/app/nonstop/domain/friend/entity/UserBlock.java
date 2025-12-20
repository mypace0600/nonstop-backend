package com.app.nonstop.domain.friend.entity;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserBlock {

    private final Long blockerId;
    private final Long blockedId;
    private final LocalDateTime createdAt;

    @Builder
    public UserBlock(Long blockerId, Long blockedId, LocalDateTime createdAt) {
        this.blockerId = blockerId;
        this.blockedId = blockedId;
        this.createdAt = createdAt;
    }
}

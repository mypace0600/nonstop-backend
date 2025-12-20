package com.app.nonstop.domain.friend.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FriendStatus {
    WAITING("대기"),
    ACCEPTED("수락"),
    REJECTED("거절"),
    BLOCKED("차단"); // DDL에 존재하므로 포함. 서비스 로직에서는 user_blocks를 우선 사용.

    private final String description;
}

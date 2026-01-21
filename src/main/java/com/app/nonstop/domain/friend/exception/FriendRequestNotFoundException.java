package com.app.nonstop.domain.friend.exception;

import com.app.nonstop.global.common.exception.ResourceNotFoundException;

public class FriendRequestNotFoundException extends ResourceNotFoundException {
    public FriendRequestNotFoundException() {
        super("존재하지 않는 친구 요청입니다.");
    }
}

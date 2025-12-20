package com.app.nonstop.domain.friend.exception;

public class FriendRequestNotFoundException extends RuntimeException {
    public FriendRequestNotFoundException() {
        super("존재하지 않는 친구 요청입니다.");
    }
}

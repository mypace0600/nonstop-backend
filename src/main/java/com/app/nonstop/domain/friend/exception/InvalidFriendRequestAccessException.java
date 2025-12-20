package com.app.nonstop.domain.friend.exception;

public class InvalidFriendRequestAccessException extends RuntimeException {
    public InvalidFriendRequestAccessException() {
        super("해당 친구 요청을 처리할 권한이 없습니다.");
    }
}

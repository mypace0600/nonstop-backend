package com.app.nonstop.domain.friend.exception;

import com.app.nonstop.global.common.exception.AccessDeniedException;

public class InvalidFriendRequestAccessException extends AccessDeniedException {
    public InvalidFriendRequestAccessException() {
        super("해당 친구 요청을 처리할 권한이 없습니다.");
    }
}

package com.app.nonstop.domain.friend.exception;

public class CannotSendFriendRequestException extends RuntimeException {
    public CannotSendFriendRequestException(String message) {
        super(message);
    }
}

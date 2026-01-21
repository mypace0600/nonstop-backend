package com.app.nonstop.domain.friend.exception;

import com.app.nonstop.global.common.exception.BusinessException;

public class CannotSendFriendRequestException extends BusinessException {
    public CannotSendFriendRequestException(String message) {
        super(message);
    }
}

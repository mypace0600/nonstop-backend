package com.app.nonstop.domain.friend.exception;

import com.app.nonstop.global.common.exception.BusinessException;

public class NotBlockedException extends BusinessException {
    public NotBlockedException() {
        super("차단하지 않은 사용자입니다.");
    }
}

package com.app.nonstop.domain.friend.exception;

import com.app.nonstop.global.common.exception.BusinessException;

public class AlreadyBlockedException extends BusinessException {
    public AlreadyBlockedException() {
        super("이미 차단한 사용자입니다.");
    }
}

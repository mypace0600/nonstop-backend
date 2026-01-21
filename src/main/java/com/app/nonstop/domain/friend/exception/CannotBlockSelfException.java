package com.app.nonstop.domain.friend.exception;

import com.app.nonstop.global.common.exception.BusinessException;

public class CannotBlockSelfException extends BusinessException {
    public CannotBlockSelfException() {
        super("자기 자신을 차단할 수 없습니다.");
    }
}

package com.app.nonstop.domain.auth.exception;

import com.app.nonstop.global.common.exception.BaseException;
import org.springframework.http.HttpStatus;

public class UnderAgeException extends BaseException {
    public UnderAgeException() {
        super(HttpStatus.BAD_REQUEST, "UNDER_AGE_LIMIT", "만 14세 미만은 서비스 이용이 제한됩니다.");
    }
}

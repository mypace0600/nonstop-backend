package com.app.nonstop.domain.auth.exception;

import com.app.nonstop.global.common.exception.BaseException;
import org.springframework.http.HttpStatus;

public class VerificationCodeExpiredException extends BaseException {
    public VerificationCodeExpiredException() {
        super(HttpStatus.BAD_REQUEST, "VERIFICATION_CODE_EXPIRED", "인증 코드가 만료되었거나 존재하지 않습니다.");
    }
}

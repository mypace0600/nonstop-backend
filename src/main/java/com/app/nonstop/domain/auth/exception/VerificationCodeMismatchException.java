package com.app.nonstop.domain.auth.exception;

import com.app.nonstop.global.common.exception.BaseException;
import org.springframework.http.HttpStatus;

public class VerificationCodeMismatchException extends BaseException {
    public VerificationCodeMismatchException() {
        super(HttpStatus.BAD_REQUEST, "VERIFICATION_CODE_MISMATCH", "인증 코드가 일치하지 않습니다.");
    }
}

package com.app.nonstop.domain.auth.exception;

public class VerificationCodeMismatchException extends RuntimeException {
    public VerificationCodeMismatchException() {
        super("인증 코드가 일치하지 않습니다.");
    }
}

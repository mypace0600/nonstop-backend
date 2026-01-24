package com.app.nonstop.domain.auth.exception;

public class VerificationCodeExpiredException extends RuntimeException {
    public VerificationCodeExpiredException() {
        super("인증 코드가 만료되었거나 존재하지 않습니다.");
    }
}

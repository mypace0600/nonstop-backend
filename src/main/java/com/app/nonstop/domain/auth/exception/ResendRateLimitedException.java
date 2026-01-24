package com.app.nonstop.domain.auth.exception;

public class ResendRateLimitedException extends RuntimeException {
    public ResendRateLimitedException() {
        super("인증 메일 재발송은 1분에 한 번만 가능합니다.");
    }
}

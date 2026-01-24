package com.app.nonstop.domain.auth.exception;

import com.app.nonstop.global.common.exception.BaseException;
import org.springframework.http.HttpStatus;

public class ResendRateLimitedException extends BaseException {
    public ResendRateLimitedException() {
        super(HttpStatus.TOO_MANY_REQUESTS, "RESEND_RATE_LIMITED", "인증 메일 재발송은 1분에 한 번만 가능합니다.");
    }
}

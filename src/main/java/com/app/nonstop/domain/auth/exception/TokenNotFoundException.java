package com.app.nonstop.domain.auth.exception;

public class TokenNotFoundException extends RuntimeException {
    public TokenNotFoundException() {
        super("토큰을 찾을 수 없습니다.");
    }

    public TokenNotFoundException(String message) {
        super(message);
    }
}

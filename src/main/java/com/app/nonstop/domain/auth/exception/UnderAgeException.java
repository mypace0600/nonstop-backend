package com.app.nonstop.domain.auth.exception;

public class UnderAgeException extends RuntimeException {
    public UnderAgeException() {
        super("만 14세 미만은 서비스 이용이 제한됩니다.");
    }
}

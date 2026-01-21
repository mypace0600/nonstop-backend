package com.app.nonstop.domain.auth.exception;

public class DuplicateEmailException extends RuntimeException {
    public DuplicateEmailException() {
        super("이미 사용중인 이메일입니다.");
    }

    public DuplicateEmailException(String message) {
        super(message);
    }
}

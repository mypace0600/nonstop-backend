package com.app.nonstop.domain.verification.exception;

public class VerificationRequestAlreadyExistsException extends RuntimeException {
    public VerificationRequestAlreadyExistsException() {
        super("이미 처리 대기 중인 학생증 인증 요청이 존재합니다.");
    }

    public VerificationRequestAlreadyExistsException(String message) {
        super(message);
    }
}

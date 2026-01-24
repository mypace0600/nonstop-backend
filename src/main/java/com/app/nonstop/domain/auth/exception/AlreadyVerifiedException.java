package com.app.nonstop.domain.auth.exception;

public class AlreadyVerifiedException extends RuntimeException {
    public AlreadyVerifiedException() {
        super("이미 이메일 인증이 완료된 사용자입니다.");
    }
}

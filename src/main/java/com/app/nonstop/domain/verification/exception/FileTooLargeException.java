package com.app.nonstop.domain.verification.exception;

public class FileTooLargeException extends RuntimeException {
    public FileTooLargeException() {
        super("파일 크기가 허용된 용량을 초과했습니다.");
    }

    public FileTooLargeException(String message) {
        super(message);
    }
}

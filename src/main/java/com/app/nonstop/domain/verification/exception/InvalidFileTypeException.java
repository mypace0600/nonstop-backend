package com.app.nonstop.domain.verification.exception;

public class InvalidFileTypeException extends RuntimeException {
    public InvalidFileTypeException() {
        super("유효하지 않은 파일 형식입니다. 이미지(JPG, PNG) 파일만 업로드 가능합니다.");
    }

    public InvalidFileTypeException(String message) {
        super(message);
    }
}

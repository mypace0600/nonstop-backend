package com.app.nonstop.domain.user.exception;

public class InvalidPasswordChangeAttemptException extends RuntimeException {
    public InvalidPasswordChangeAttemptException() {
        super("소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다.");
    }
}

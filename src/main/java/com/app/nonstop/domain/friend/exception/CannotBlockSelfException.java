package com.app.nonstop.domain.friend.exception;

public class CannotBlockSelfException extends RuntimeException {
    public CannotBlockSelfException() {
        super("자기 자신을 차단할 수 없습니다.");
    }
}

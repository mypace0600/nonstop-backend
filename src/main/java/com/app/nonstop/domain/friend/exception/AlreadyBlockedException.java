package com.app.nonstop.domain.friend.exception;

public class AlreadyBlockedException extends RuntimeException {
    public AlreadyBlockedException() {
        super("이미 차단한 사용자입니다.");
    }
}

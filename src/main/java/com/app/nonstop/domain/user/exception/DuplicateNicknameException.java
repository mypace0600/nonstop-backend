package com.app.nonstop.domain.user.exception;

// TODO: 추후에 프로젝트 공통 예외 응답 포맷이 정해지면, ErrorCode 등을 사용하는 형태로 리팩토링할 수 있습니다.
public class DuplicateNicknameException extends RuntimeException {
    public DuplicateNicknameException() {
        super("이미 사용 중인 닉네임입니다.");
    }

    public DuplicateNicknameException(String message) {
        super(message);
    }
}

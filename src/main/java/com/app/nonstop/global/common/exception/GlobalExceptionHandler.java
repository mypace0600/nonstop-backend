package com.app.nonstop.global.common.exception;

import com.app.nonstop.domain.auth.exception.*;
import com.app.nonstop.domain.user.exception.InvalidPasswordException;
import com.app.nonstop.domain.user.exception.UserNotFoundException;
import com.app.nonstop.global.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PolicyAgreementRequiredException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<?> handlePolicyAgreementRequiredException(PolicyAgreementRequiredException e) {
        return ApiResponse.error(e.getMessage(), e.getRequiredPolicies());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<?> handleResourceNotFoundException(ResourceNotFoundException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<?> handleAccessDeniedException(AccessDeniedException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleBusinessException(BusinessException e) {
        return ApiResponse.error(e.getMessage());
    }

    // Auth 관련 예외 (401 Unauthorized)
    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<?> handleUserNotFoundException(UserNotFoundException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(InvalidPasswordException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<?> handleInvalidPasswordException(InvalidPasswordException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(InvalidTokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<?> handleInvalidTokenException(InvalidTokenException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(TokenNotFoundException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<?> handleTokenNotFoundException(TokenNotFoundException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(ExpiredTokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<?> handleExpiredTokenException(ExpiredTokenException e) {
        return ApiResponse.error(e.getMessage());
    }

    // 이메일 중복 (409 Conflict)
    @ExceptionHandler(DuplicateEmailException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ApiResponse<?> handleDuplicateEmailException(DuplicateEmailException e) {
        return ApiResponse.error(e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        return ApiResponse.error(message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<?> handleException(Exception e) {
        // 실제 운영 시에는 로깅 필요 (e.printStackTrace() 대신 log.error())
        return ApiResponse.error("서버 내부 오류가 발생했습니다.");
    }
}

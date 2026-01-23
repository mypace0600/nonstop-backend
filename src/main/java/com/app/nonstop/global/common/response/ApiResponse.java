package com.app.nonstop.global.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

/**
 * API 응답을 위한 표준 래퍼(Wrapper) 클래스입니다.
 * 모든 API는 이 클래스를 통해 응답을 반환하여, 클라이언트가 항상 일관된 구조의 응답을 받도록 합니다.
 *
 * @param <T> 응답 데이터의 타입을 나타내는 제네릭
 */
@Getter
public class ApiResponse<T> {

    /**
     * API 호출 성공 여부
     */
    private final boolean success;

    /**
     * 실제 응답 데이터 (성공 시)
     */
    private final T data;

    /**
     * 에러 메시지 (실패 시)
     * JsonInclude.Include.NON_EMPTY: 필드가 비어있지 않을 때만 JSON에 포함시킵니다.
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final String message;

    private ApiResponse(boolean success, T data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }

    /**
     * 성공 응답을 생성합니다. (데이터 포함)
     *
     * @param data 포함할 데이터
     * @return ApiResponse 객체
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }
    
    /**
     * 성공 응답을 생성합니다. (데이터 없음)
     *
     * @return ApiResponse 객체
     */
    public static ApiResponse<?> success() {
        return new ApiResponse<>(true, null, null);
    }

    /**
     * 실패 응답을 생성합니다. (데이터 포함)
     *
     * @param message 실패 메시지
     * @param data 포함할 데이터
     * @return ApiResponse 객체
     */
    public static <T> ApiResponse<T> error(String message, T data) {
        return new ApiResponse<>(false, data, message);
    }

    /**
     * 실패 응답을 생성합니다.
     *
     * @param message 실패 메시지
     * @return ApiResponse 객체
     */
    public static ApiResponse<?> error(String message) {
        return new ApiResponse<>(false, null, message);
    }
}

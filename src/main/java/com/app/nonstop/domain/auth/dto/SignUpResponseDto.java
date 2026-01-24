package com.app.nonstop.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "회원가입 응답 DTO")
public class SignUpResponseDto {

    @Schema(description = "사용자 ID", example = "123")
    private Long userId;

    @Schema(description = "이메일", example = "test@example.com")
    private String email;
}

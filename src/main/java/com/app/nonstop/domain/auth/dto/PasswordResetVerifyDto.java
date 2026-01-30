package com.app.nonstop.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "비밀번호 재설정 코드 확인 DTO")
public class PasswordResetVerifyDto {

    @NotBlank(message = "이메일은 필수 입력 항목입니다.")
    @Email(message = "유효한 이메일 형식이 아닙니다.")
    @Schema(description = "이메일", example = "test@example.com")
    private String email;

    @NotBlank(message = "인증 코드는 필수 입력 항목입니다.")
    @Schema(description = "인증 코드", example = "123456")
    private String code;
}

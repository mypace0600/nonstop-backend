package com.app.nonstop.domain.verification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EmailVerificationConfirmDto {
    @Schema(description = "인증 코드", example = "123456")
    @NotBlank
    private String code;
}

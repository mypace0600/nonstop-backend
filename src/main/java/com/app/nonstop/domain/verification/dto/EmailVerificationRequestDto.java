package com.app.nonstop.domain.verification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EmailVerificationRequestDto {
    @Schema(description = "학교 웹메일 주소", example = "student@korea.ac.kr")
    @NotBlank
    @Email
    private String email;
}

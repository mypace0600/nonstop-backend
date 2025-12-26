package com.app.nonstop.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "Access Token 재발급 요청 DTO")
public class RefreshRequestDto {

    @NotBlank(message = "Refresh Token은 필수입니다.")
    @Schema(description = "Refresh Token")
    private String refreshToken;
}

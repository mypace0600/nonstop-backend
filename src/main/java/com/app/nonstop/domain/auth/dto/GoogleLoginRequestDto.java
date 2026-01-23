package com.app.nonstop.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "Google 로그인 요청 DTO")
public class GoogleLoginRequestDto {

    @NotBlank(message = "Google ID 토큰은 필수입니다.")
    @Schema(description = "Google ID 토큰", example = "eyJhbG...")
    private String idToken;

    @Schema(description = "동의한 정책 ID 목록", example = "[1, 2]")
    private java.util.List<Long> agreedPolicyIds;
}

package com.app.nonstop.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "로그인 성공 응답 DTO")
public class TokenResponseDto {

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "Access Token")
    private String accessToken;

    @Schema(description = "Refresh Token")
    private String refreshToken;

    @Schema(description = "이메일 인증 여부")
    private Boolean emailVerified;

    @Schema(description = "필수 정책 동의 완료 여부", example = "true")
    private Boolean hasAgreedAllMandatory;

    @Schema(description = "생년월일 등록 여부", example = "true")
    private Boolean hasBirthDate;
}

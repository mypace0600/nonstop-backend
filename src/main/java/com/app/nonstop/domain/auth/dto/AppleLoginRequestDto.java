package com.app.nonstop.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "Apple 로그인 요청 DTO")
public class AppleLoginRequestDto {

    @NotBlank(message = "Apple ID 토큰은 필수입니다.")
    @Schema(description = "Firebase ID 토큰 (Apple Sign In으로 인증 후 Firebase에서 발급)", example = "eyJhbG...")
    private String idToken;

    @Schema(description = "Apple Authorization Code (선택)", example = "c1234...")
    private String authorizationCode;

    @Schema(description = "사용자 이름 (최초 로그인 시에만 제공)", example = "John")
    private String firstName;

    @Schema(description = "사용자 성 (최초 로그인 시에만 제공)", example = "Doe")
    private String lastName;
}

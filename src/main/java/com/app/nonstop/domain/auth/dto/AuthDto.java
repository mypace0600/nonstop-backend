package com.app.nonstop.domain.auth.dto;

import com.app.nonstop.domain.user.entity.AuthProvider;
import com.app.nonstop.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

public class AuthDto {
    @Getter
    @NoArgsConstructor
    @Schema(description = "이메일 회원가입 요청 DTO")
    public static class SignUpRequest {

        @NotBlank(message = "이메일은 필수 입력 항목입니다.")
        @Email(message = "유효한 이메일 형식이 아닙니다.")
        @Schema(description = "이메일", example = "test@example.com")
        private String email;

        @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
        @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하로 입력해주세요.")
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$",
                message = "비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 합니다.")
        @Schema(description = "비밀번호", example = "password123!")
        private String password;

        @NotBlank(message = "닉네임은 필수 입력 항목입니다.")
        @Size(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하로 입력해주세요.")
        @Schema(description = "닉네임", example = "테스트유저")
        private String nickname;

        public User toEntity(PasswordEncoder passwordEncoder) {
            return User.builder()
                    .email(this.email)
                    .password(passwordEncoder.encode(this.password))
                    .nickname(this.nickname)
                    .authProvider(AuthProvider.EMAIL)
                    .build();
        }
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "이메일 로그인 요청 DTO")
    public static class LoginRequest {

        @NotBlank(message = "이메일은 필수 입력 항목입니다.")
        @Email(message = "유효한 이메일 형식이 아닙니다.")
        @Schema(description = "이메일", example = "test@example.com")
        private String email;

        @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
        @Schema(description = "비밀번호", example = "password123!")
        private String password;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "Google 로그인 요청 DTO")
    public static class GoogleLoginRequest {
        @NotBlank(message = "Google ID 토큰은 필수입니다.")
        @Schema(description = "Google ID 토큰", example = "eyJhbGciOiJSUzI1NiIsImtpZCI6Im...")
        private String idToken;
    }


    @Getter
    @Builder
    @Schema(description = "인증 토큰 응답 DTO")
    public static class TokenResponse {
        @Schema(description = "Access Token")
        private String accessToken;

        @Schema(description = "Refresh Token")
        private String refreshToken;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "Access Token 재발급 요청 DTO")
    public static class RefreshRequest {
        @NotBlank(message = "Refresh Token은 필수입니다.")
        @Schema(description = "Refresh Token")
        private String refreshToken;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "이메일 중복 확인 요청 DTO")
    public static class EmailCheckRequest {
        @NotBlank(message = "이메일은 필수 입력 항목입니다.")
        @Email(message = "유효한 이메일 형식이 아닙니다.")
        @Schema(description = "이메일", example = "test@example.com")
        private String email;
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "닉네임 중복 확인 요청 DTO")
    public static class NicknameCheckRequest {
        @NotBlank(message = "닉네임은 필수 입력 항목입니다.")
        @Size(min = 2, max = 10, message = "닉네임은 2자 이상 10자 이하로 입력해주세요.")
        @Schema(description = "닉네임", example = "테스트유저")
        private String nickname;
    }
}

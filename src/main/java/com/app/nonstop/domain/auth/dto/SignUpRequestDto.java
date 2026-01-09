package com.app.nonstop.domain.auth.dto;

import com.app.nonstop.domain.user.entity.AuthProvider;
import com.app.nonstop.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;

@Getter
@NoArgsConstructor
@Schema(description = "이메일 회원가입 요청 DTO")
public class SignUpRequestDto {

    @NotBlank(message = "이메일은 필수 입력 항목입니다.")
    @Email(message = "유효한 이메일 형식이 아닙니다.")
    @Schema(description = "이메일", example = "test@example.com")
    private String email;

    @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
    @Size(min = 6, max = 20, message = "비밀번호는 6자 이상 20자 이하로 입력해주세요.")
    @Schema(description = "비밀번호", example = "password123!")
    private String password;

    @NotBlank(message = "닉네임은 필수 입력 항목입니다.")
    @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하로 입력해주세요.")
    @Schema(description = "닉네임", example = "테스트유저")
    private String nickname;

    @Schema(description = "대학교 ID", example = "1")
    private Long universityId;

    @Schema(description = "전공 ID", example = "1")
    private Long majorId;

    public User toEntity(PasswordEncoder passwordEncoder) {
        return User.builder()
                .email(this.email)
                .password(passwordEncoder.encode(this.password))
                .nickname(this.nickname)
                .authProvider(AuthProvider.EMAIL)
                .universityId(this.universityId)
                .majorId(this.majorId)
                .build();
    }
}

package com.app.nonstop.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "비밀번호 변경 요청 DTO")
public class PasswordUpdateRequestDto {

    @Schema(description = "현재 비밀번호", example = "password123!")
    @NotBlank(message = "현재 비밀번호를 입력해주세요.")
    private String currentPassword;

    // TODO: 프론트엔드와 협의하여 더 구체적인 비밀번호 정책(예: 최소/최대 길이, 특수문자 포함 등)을 적용해야 합니다.
    @Schema(description = "새 비밀번호", example = "newPassword123!")
    @NotBlank(message = "새 비밀번호를 입력해주세요.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{8,$", message = "비밀번호는 8자 이상이며, 영문자와 숫자를 최소 1개 이상 포함해야 합니다.")
    private String newPassword;
}

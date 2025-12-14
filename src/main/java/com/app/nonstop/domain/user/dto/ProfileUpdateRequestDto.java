package com.app.nonstop.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "프로필 수정 요청 DTO")
public class ProfileUpdateRequestDto {

    @Schema(description = "새 닉네임", example = "새로운나")
    @Size(min = 2, max = 30, message = "닉네임은 2자 이상 30자 이하로 입력해주세요.")
    private String nickname;

    @Schema(description = "대학 ID", example = "10")
    private Long universityId;

    @Schema(description = "전공 ID", example = "100")
    private Long majorId;

    // TODO: 추후 파일 스토리지(예: S3) Presigned URL을 받는 로직으로 변경될 수 있습니다.
    @Schema(description = "새 프로필 이미지 URL", example = "https://example.com/new_profile.jpg")
    private String profileImageUrl;

    @Schema(description = "자기소개", example = "만나서 반갑습니다.")
    @Size(max = 500, message = "자기소개는 500자 이하로 입력해주세요.")
    private String introduction;

    @Schema(description = "선호 언어", example = "en")
    private String preferredLanguage;

}

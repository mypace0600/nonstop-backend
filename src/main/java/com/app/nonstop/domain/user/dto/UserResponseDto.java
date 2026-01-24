package com.app.nonstop.domain.user.dto;

import com.app.nonstop.domain.user.entity.User;
import com.app.nonstop.domain.user.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "내 정보 조회 응답 DTO")
public class UserResponseDto {

    @Schema(description = "사용자 ID (legacy)", example = "1")
    private Long id;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "이메일", example = "test@example.com")
    private String email;

    @Schema(description = "닉네임", example = "논스톱")
    private String nickname;

    @Schema(description = "대학 ID", example = "10")
    private Long universityId;

    @Schema(description = "전공 ID", example = "100")
    private Long majorId;

    // TODO: 추후 파일 스토리지(예: S3) 구성이 완료되면, 실제 URL 형식에 맞는 예시로 변경해야 합니다.
    @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
    private String profileImageUrl;

    @Schema(description = "자기소개", example = "안녕하세요!")
    private String introduction;

    @Schema(description = "선호 언어", example = "ko")
    private String preferredLanguage;

    @Schema(description = "대학생 인증 여부", example = "true")
    private Boolean isVerified;

    @Schema(description = "사용자 권한", example = "USER")
    private UserRole userRole;

    @Schema(description = "생년월일", example = "2000-01-01")
    private java.time.LocalDate birthDate;

    public static UserResponseDto of(User user) {
        return UserResponseDto.builder()
                .userId(user.getId())
                .id(user.getId())
                .userId(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .universityId(user.getUniversityId())
                .majorId(user.getMajorId())
                .profileImageUrl(user.getProfileImageUrl())
                .introduction(user.getIntroduction())
                .preferredLanguage(user.getPreferredLanguage())
                .isVerified(user.getIsVerified())
                .userRole(user.getUserRole())
                .birthDate(user.getBirthDate())
                .build();
    }
}

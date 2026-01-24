package com.app.nonstop.domain.admin.dto;

import com.app.nonstop.domain.user.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "관리자용 사용자 정보")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDto {

    @Schema(description = "사용자 ID", example = "1")
    private Long id;

    @Schema(description = "이메일", example = "user@example.com")
    private String email;

    @Schema(description = "닉네임", example = "홍길동")
    private String nickname;

    @Schema(description = "대학교명 (인증된 경우)", example = "서울대학교")
    private String universityName;

    @Schema(description = "전공명 (인증된 경우)", example = "컴퓨터공학과")
    private String majorName;

    @Schema(description = "사용자 권한", example = "ROLE_USER")
    private UserRole userRole;

    @Schema(description = "계정 활성화 상태", example = "true")
    private Boolean isActive;

    @Schema(description = "학교 인증 여부", example = "false")
    private Boolean isUniversityVerified;

    @Schema(description = "가입 일시", example = "2025-01-01T09:00:00")
    private LocalDateTime createdAt;
}

package com.app.nonstop.domain.admin.dto;

import com.app.nonstop.domain.report.entity.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "관리자용 학교 인증 요청 정보")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminVerificationDto {

    @Schema(description = "인증 요청 ID", example = "1")
    private Long id;

    @Schema(description = "요청자 사용자 ID", example = "42")
    private Long userId;

    @Schema(description = "요청자 닉네임", example = "홍길동")
    private String userNickname;

    @Schema(description = "대학교명", example = "서울대학교")
    private String universityName;

    @Schema(description = "전공명", example = "컴퓨터공학과")
    private String majorName;

    @Schema(description = "학생증/재학증명서 이미지 URL", example = "https://storage.example.com/verifications/abc123.jpg")
    private String imageUrl;

    @Schema(description = "인증 상태", example = "PENDING")
    private ReportStatus status;

    @Schema(description = "제출 일시", example = "2025-01-15T10:30:00")
    private LocalDateTime submittedAt;
}

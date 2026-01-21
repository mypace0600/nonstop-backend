package com.app.nonstop.domain.admin.dto;

import com.app.nonstop.domain.report.entity.ReportReasonType;
import com.app.nonstop.domain.report.entity.ReportStatus;
import com.app.nonstop.domain.report.entity.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "관리자용 신고 정보")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminReportDto {

    @Schema(description = "신고 ID", example = "1")
    private Long id;

    @Schema(description = "신고자 닉네임", example = "김철수")
    private String reporterNickname;

    @Schema(description = "신고 대상 유형", example = "POST")
    private ReportTargetType targetType;

    @Schema(description = "신고 대상 ID (게시글/댓글/채팅방 ID)", example = "123")
    private Long targetId;

    @Schema(description = "신고 사유", example = "SPAM")
    private ReportReasonType reason;

    @Schema(description = "신고 상세 설명", example = "반복적으로 광고성 게시글을 올리고 있습니다.")
    private String description;

    @Schema(description = "신고 처리 상태", example = "PENDING")
    private ReportStatus status;

    @Schema(description = "신고 접수 일시", example = "2025-01-15T14:20:00")
    private LocalDateTime createdAt;
}

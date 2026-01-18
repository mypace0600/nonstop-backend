package com.app.nonstop.domain.admin.dto;

import com.app.nonstop.domain.report.entity.ReportReasonType;
import com.app.nonstop.domain.report.entity.ReportStatus;
import com.app.nonstop.domain.report.entity.ReportTargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminReportDto {
    private Long id;
    private String reporterNickname;
    private ReportTargetType targetType;
    private Long targetId;
    private ReportReasonType reason;
    private String description;
    private ReportStatus status;
    private LocalDateTime createdAt;
}

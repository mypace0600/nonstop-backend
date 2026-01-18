package com.app.nonstop.domain.admin.dto;

import com.app.nonstop.domain.report.entity.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminVerificationDto {
    private Long id;
    private Long userId;
    private String userNickname;
    private String universityName;
    private String majorName;
    private String imageUrl;
    private ReportStatus status;
    private LocalDateTime submittedAt;
}

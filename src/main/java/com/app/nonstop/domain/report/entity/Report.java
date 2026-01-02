package com.app.nonstop.domain.report.entity;

import com.app.nonstop.domain.report.entity.ReportStatus;
import com.app.nonstop.global.common.entity.BaseTimeEntity;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Report extends BaseTimeEntity {
    private Long id;
    private Long reporterId;
    private ReportTargetType targetType;
    private Long targetId;
    private ReportReasonType reason;
    private String description;
    private ReportStatus status;
    private Long handledBy;
    private LocalDateTime handledAt;
}

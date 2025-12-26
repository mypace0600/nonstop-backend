package com.app.nonstop.domain.report.service;

import com.app.nonstop.domain.report.dto.ReportDto;
import com.app.nonstop.domain.report.entity.Report;
import com.app.nonstop.domain.report.entity.ReportTargetType;
import com.app.nonstop.domain.report.mapper.ReportMapper;
import com.app.nonstop.domain.report.entity.ReportStatus;
import com.app.nonstop.global.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 신고 처리 비즈니스 로직.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportMapper reportMapper;

    /**
     * 대상을 신고합니다.
     * 중복 신고는 허용하지 않습니다.
     *
     * @param reporterId 신고자 ID
     * @param targetType 신고 대상 유형 (POST, COMMENT 등)
     * @param targetId   신고 대상 ID
     * @param requestDto 신고 상세 내용
     */
    @Transactional
    public void createReport(Long reporterId, ReportTargetType targetType, Long targetId, ReportDto.Request requestDto) {
        // 중복 신고 체크
        if (reportMapper.existsByReporterAndTarget(reporterId, targetType.name(), targetId)) {
            throw new BusinessException("이미 신고한 대상입니다.");
        }

        Report report = new Report();
        report.setReporterId(reporterId);
        report.setTargetType(targetType);
        report.setTargetId(targetId);
        report.setReason(requestDto.getReason());
        report.setDescription(requestDto.getDescription());
        report.setStatus(ReportStatus.PENDING);

        reportMapper.insert(report);
    }
}

package com.app.nonstop.domain.report.dto;

import com.app.nonstop.domain.report.entity.ReportReasonType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 신고 관련 DTO 관리 클래스.
 */
public class ReportDto {

    /**
     * 신고 요청.
     */
    @Getter
    @Setter
    public static class Request {
        @NotNull(message = "신고 사유는 필수입니다.")
        private ReportReasonType reason;

        private String description;
    }
}

package com.app.nonstop.domain.report.entity;

/**
 * 학생증 인증 요청 및 신고 처리 상태를 나타내는 Enum.
 *
 * PENDING: 처리 대기 중
 * REVIEWED: 관리자가 검토 완료
 * ACTION_TAKEN: 조치 완료 (예: 인증 승인)
 * REJECTED: 반려
 */
public enum ReportStatus {
    PENDING,
    REVIEWED,
    ACTION_TAKEN,
    REJECTED
}

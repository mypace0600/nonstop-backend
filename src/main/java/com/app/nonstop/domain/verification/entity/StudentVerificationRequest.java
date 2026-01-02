package com.app.nonstop.domain.verification.entity;

import com.app.nonstop.domain.report.entity.ReportStatus;
import com.app.nonstop.domain.user.entity.User;
import com.app.nonstop.global.common.entity.BaseTimeEntity;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 학생증 인증 요청 정보를 나타내는 데이터 객체(POJO).
 * `student_verification_requests` 테이블의 레코드와 매핑됩니다.
 */
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class StudentVerificationRequest extends BaseTimeEntity {

    private Long id;
    private User user; // user_id (FK)
    private String imageUrl;
    private ReportStatus status;
    private String rejectReason;
    private User reviewedBy; // reviewed_by (FK)
    private LocalDateTime reviewedAt;

    @Builder
    public StudentVerificationRequest(User user, String imageUrl, ReportStatus status) {
        this.user = user;
        this.imageUrl = imageUrl;
        this.status = status;
    }
}

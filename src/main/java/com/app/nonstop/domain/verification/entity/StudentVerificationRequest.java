package com.app.nonstop.domain.verification.entity;

import com.app.nonstop.domain.user.entity.User;
import com.app.nonstop.global.common.entity.BaseTimeEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class StudentVerificationRequest extends BaseTimeEntity {

    private Long id;
    private User user;
    private String imageUrl;
    private ReportStatus status;
    private String rejectReason;
    private User reviewedBy;
    private LocalDateTime reviewedAt;

    @Builder
    public StudentVerificationRequest(User user, String imageUrl, ReportStatus status) {
        this.user = user;
        this.imageUrl = imageUrl;
        this.status = status;
    }
}

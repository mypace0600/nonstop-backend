package com.app.nonstop.domain.user.entity;

import com.app.nonstop.domain.major.entity.Major;
import com.app.nonstop.domain.university.entity.University;
import com.app.nonstop.global.common.entity.BaseTimeEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자 정보를 나타내는 데이터 객체(POJO).
 * `users` 테이블의 레코드와 매핑됩니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseTimeEntity {

    private Long id;
    private UserRole userRole;
    private String email;
    private String password;
    private AuthProvider authProvider;
    private String providerId;
    private String nickname;
    private String studentNumber;
    private Long universityId;
    private Long majorId;
    private University university;
    private Major major;
    private String profileImageUrl;
    private String introduction;
    private String preferredLanguage;
    private Boolean isActive;
    private Boolean isVerified;
    private VerificationMethod verificationMethod;
    private LocalDateTime lastLoginAt;
    private LocalDateTime deletedAt;
}

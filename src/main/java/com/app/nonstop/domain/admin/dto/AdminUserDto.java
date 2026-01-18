package com.app.nonstop.domain.admin.dto;

import com.app.nonstop.domain.user.entity.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDto {
    private Long id;
    private String email;
    private String nickname;
    private String universityName;
    private String majorName;
    private UserRole userRole;
    private Boolean isActive;
    private Boolean isVerified;
    private LocalDateTime createdAt;
}

package com.app.nonstop.domain.admin.service;

import com.app.nonstop.domain.admin.dto.AdminReportDto;
import com.app.nonstop.domain.admin.dto.AdminUserDto;
import com.app.nonstop.domain.admin.dto.AdminVerificationDto;
import com.app.nonstop.domain.user.entity.UserRole;

import java.util.List;

public interface AdminService {
    // Verification
    List<AdminVerificationDto> getPendingVerifications(int page, int size);
    void approveVerification(Long verificationId, Long adminId);
    void rejectVerification(Long verificationId, String reason, Long adminId);

    // Report
    List<AdminReportDto> getReports(int page, int size);
    void processReport(Long reportId, String action, String memo, Long adminId);

    // User
    List<AdminUserDto> getUsers(int page, int size, String search);
    void updateUserRole(Long userId, UserRole role);
    void updateUserStatus(Long userId, boolean isActive);
}

package com.app.nonstop.domain.admin.mapper;

import com.app.nonstop.domain.admin.dto.AdminReportDto;
import com.app.nonstop.domain.admin.dto.AdminUserDto;
import com.app.nonstop.domain.admin.dto.AdminVerificationDto;
import com.app.nonstop.domain.report.entity.ReportStatus;
import com.app.nonstop.domain.user.entity.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminMapper {

    // Verification
    List<AdminVerificationDto> selectPendingVerifications(@Param("limit") int limit, @Param("offset") int offset);
    
    int updateVerificationStatus(@Param("id") Long id, @Param("status") ReportStatus status, @Param("rejectReason") String rejectReason, @Param("adminId") Long adminId);
    
    Long getUserIdByVerificationId(Long verificationId);
    
    int updateUserVerificationStatus(@Param("userId") Long userId, @Param("isUniversityVerified") boolean isUniversityVerified);

    // Report
    List<AdminReportDto> selectReports(@Param("limit") int limit, @Param("offset") int offset);

    AdminReportDto selectReportById(@Param("id") Long id);
    
    int updateReportStatus(@Param("id") Long id, @Param("status") ReportStatus status, @Param("adminId") Long adminId);

    // User
    List<AdminUserDto> selectUsers(@Param("limit") int limit, @Param("offset") int offset, @Param("search") String search);
    
    int updateUserRole(@Param("id") Long id, @Param("role") UserRole role);
    
    int updateUserStatus(@Param("id") Long id, @Param("isActive") boolean isActive);
}

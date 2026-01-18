package com.app.nonstop.domain.admin.service;

import com.app.nonstop.domain.admin.dto.AdminReportDto;
import com.app.nonstop.domain.admin.dto.AdminUserDto;
import com.app.nonstop.domain.admin.dto.AdminVerificationDto;
import com.app.nonstop.domain.admin.mapper.AdminMapper;
import com.app.nonstop.domain.community.entity.Comment;
import com.app.nonstop.domain.community.entity.Post;
import com.app.nonstop.domain.report.entity.ReportStatus;
import com.app.nonstop.domain.report.entity.ReportTargetType;
import com.app.nonstop.domain.user.entity.UserRole;
import com.app.nonstop.mapper.CommentMapper;
import com.app.nonstop.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminServiceImpl implements AdminService {

    private final AdminMapper adminMapper;
    private final PostMapper postMapper;
    private final CommentMapper commentMapper;

    @Override
    @Transactional(readOnly = true)
    public List<AdminVerificationDto> getPendingVerifications(int page, int size) {
        int offset = page * size;
        return adminMapper.selectPendingVerifications(size, offset);
    }

    @Override
    public void approveVerification(Long verificationId, Long adminId) {
        // 1. Update verification request status
        adminMapper.updateVerificationStatus(verificationId, ReportStatus.ACTION_TAKEN, null, adminId);
        
        // 2. Update user status
        Long userId = adminMapper.getUserIdByVerificationId(verificationId);
        if (userId != null) {
            adminMapper.updateUserVerificationStatus(userId, true);
        }
    }

    @Override
    public void rejectVerification(Long verificationId, String reason, Long adminId) {
        adminMapper.updateVerificationStatus(verificationId, ReportStatus.REJECTED, reason, adminId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminReportDto> getReports(int page, int size) {
        int offset = page * size;
        return adminMapper.selectReports(size, offset);
    }

    @Override
    public void processReport(Long reportId, String action, String memo, Long adminId) {
        if ("BLIND".equalsIgnoreCase(action)) {
            adminMapper.updateReportStatus(reportId, ReportStatus.ACTION_TAKEN, adminId);
            
            // Blind Content
            AdminReportDto report = adminMapper.selectReportById(reportId);
            if (report != null) {
                blindContent(report);
            }
        } else if ("REJECT".equalsIgnoreCase(action)) {
            adminMapper.updateReportStatus(reportId, ReportStatus.REJECTED, adminId);
        } else {
            throw new IllegalArgumentException("Unknown action: " + action);
        }
    }

    private void blindContent(AdminReportDto report) {
        if (report.getTargetType() == ReportTargetType.POST) {
            Post post = postMapper.findById(report.getTargetId()).orElse(null);
            if (post != null) {
                post.setContent("BLINDED BY ADMIN");
                post.setTitle("BLINDED BY ADMIN"); // Also blind title? Maybe.
                postMapper.update(post);
            }
        } else if (report.getTargetType() == ReportTargetType.COMMENT) {
            Comment comment = commentMapper.findById(report.getTargetId()).orElse(null);
            if (comment != null) {
                comment.setContent("BLINDED BY ADMIN");
                commentMapper.update(comment);
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminUserDto> getUsers(int page, int size, String search) {
        int offset = page * size;
        return adminMapper.selectUsers(size, offset, search);
    }

    @Override
    public void updateUserRole(Long userId, UserRole role) {
        adminMapper.updateUserRole(userId, role);
    }

    @Override
    public void updateUserStatus(Long userId, boolean isActive) {
        adminMapper.updateUserStatus(userId, isActive);
    }
}

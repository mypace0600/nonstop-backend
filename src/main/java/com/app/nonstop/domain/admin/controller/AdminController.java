package com.app.nonstop.domain.admin.controller;

import com.app.nonstop.domain.admin.dto.AdminReportDto;
import com.app.nonstop.domain.admin.dto.AdminUserDto;
import com.app.nonstop.domain.admin.dto.AdminVerificationDto;
import com.app.nonstop.domain.admin.service.AdminService;
import com.app.nonstop.domain.user.entity.UserRole;
import com.app.nonstop.global.security.user.CustomUserDetails;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // --- Verification ---
    @GetMapping("/verifications")
    public ResponseEntity<List<AdminVerificationDto>> getPendingVerifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminService.getPendingVerifications(page, size));
    }

    @PostMapping("/verifications/{id}/approve")
    public ResponseEntity<Void> approveVerification(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        adminService.approveVerification(id, userDetails.getUserId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/verifications/{id}/reject")
    public ResponseEntity<Void> rejectVerification(
            @PathVariable Long id,
            @RequestBody RejectVerificationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        adminService.rejectVerification(id, request.getReason(), userDetails.getUserId());
        return ResponseEntity.ok().build();
    }

    @Getter @Setter @NoArgsConstructor
    public static class RejectVerificationRequest {
        private String reason;
    }

    // --- Report ---
    @GetMapping("/reports")
    public ResponseEntity<List<AdminReportDto>> getReports(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminService.getReports(page, size));
    }

    @PostMapping("/reports/{id}/process")
    public ResponseEntity<Void> processReport(
            @PathVariable Long id,
            @RequestBody ProcessReportRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        adminService.processReport(id, request.getAction(), request.getMemo(), userDetails.getUserId());
        return ResponseEntity.ok().build();
    }

    @Getter @Setter @NoArgsConstructor
    public static class ProcessReportRequest {
        private String action; // BLIND, REJECT
        private String memo;
    }

    // --- User ---
    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDto>> getUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(adminService.getUsers(page, size, search));
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<Void> updateUserRole(
            @PathVariable Long id,
            @RequestBody UpdateUserRoleRequest request
    ) {
        adminService.updateUserRole(id, request.getRole());
        return ResponseEntity.ok().build();
    }

    @Getter @Setter @NoArgsConstructor
    public static class UpdateUserRoleRequest {
        private UserRole role;
    }

    @PatchMapping("/users/{id}/status")
    public ResponseEntity<Void> updateUserStatus(
            @PathVariable Long id,
            @RequestBody UpdateUserStatusRequest request
    ) {
        adminService.updateUserStatus(id, request.getIsActive());
        return ResponseEntity.ok().build();
    }

    @Getter @Setter @NoArgsConstructor
    public static class UpdateUserStatusRequest {
        private Boolean isActive;
    }
}

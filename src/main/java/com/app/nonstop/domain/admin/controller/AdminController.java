package com.app.nonstop.domain.admin.controller;

import com.app.nonstop.domain.admin.dto.AdminReportDto;
import com.app.nonstop.domain.admin.dto.AdminUserDto;
import com.app.nonstop.domain.admin.dto.AdminVerificationDto;
import com.app.nonstop.domain.admin.service.AdminService;
import com.app.nonstop.domain.user.entity.UserRole;
import com.app.nonstop.global.common.response.ApiResponse;
import com.app.nonstop.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Admin", description = "관리자 전용 API - 학교 인증 승인/거절, 신고 처리, 사용자 관리")
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ==================== Verification (학교 인증) ====================

    @Operation(
            summary = "대기 중인 학교 인증 목록 조회",
            description = "PENDING 상태인 학교 인증 요청 목록을 페이지네이션하여 조회합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "관리자 권한 필요",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @GetMapping("/verifications")
    public ResponseEntity<List<AdminVerificationDto>> getPendingVerifications(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminService.getPendingVerifications(page, size));
    }

    @Operation(
            summary = "학교 인증 승인",
            description = "학교 인증 요청을 승인합니다. 승인 시 해당 사용자의 isVerified가 true로 변경됩니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "승인 완료"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "관리자 권한 필요",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "인증 요청을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PostMapping("/verifications/{id}/approve")
    public ResponseEntity<Void> approveVerification(
            @Parameter(description = "인증 요청 ID", example = "1", required = true)
            @PathVariable Long id,
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        adminService.approveVerification(id, userDetails.getUserId());
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "학교 인증 거절",
            description = "학교 인증 요청을 거절합니다. 거절 사유를 함께 전달합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "거절 완료"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "관리자 권한 필요",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "인증 요청을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PostMapping("/verifications/{id}/reject")
    public ResponseEntity<Void> rejectVerification(
            @Parameter(description = "인증 요청 ID", example = "1", required = true)
            @PathVariable Long id,
            @RequestBody RejectVerificationRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        adminService.rejectVerification(id, request.getReason(), userDetails.getUserId());
        return ResponseEntity.ok().build();
    }

    @Schema(description = "학교 인증 거절 요청")
    @Getter @Setter @NoArgsConstructor
    public static class RejectVerificationRequest {
        @Schema(description = "거절 사유", example = "학생증 이미지가 불명확합니다. 재촬영 후 다시 제출해주세요.")
        private String reason;
    }

    // ==================== Report (신고) ====================

    @Operation(
            summary = "신고 목록 조회",
            description = "접수된 신고 목록을 페이지네이션하여 조회합니다. 최신순으로 정렬됩니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "관리자 권한 필요",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @GetMapping("/reports")
    public ResponseEntity<List<AdminReportDto>> getReports(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(adminService.getReports(page, size));
    }

    @Operation(
            summary = "신고 처리",
            description = "신고를 처리합니다. BLIND(콘텐츠 블라인드) 또는 REJECT(신고 기각) 중 선택합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "처리 완료"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "관리자 권한 필요",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "신고를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PostMapping("/reports/{id}/process")
    public ResponseEntity<Void> processReport(
            @Parameter(description = "신고 ID", example = "1", required = true)
            @PathVariable Long id,
            @RequestBody ProcessReportRequest request,
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        adminService.processReport(id, request.getAction(), request.getMemo(), userDetails.getUserId());
        return ResponseEntity.ok().build();
    }

    @Schema(description = "신고 처리 요청")
    @Getter @Setter @NoArgsConstructor
    public static class ProcessReportRequest {
        @Schema(description = "처리 액션", example = "BLIND", allowableValues = {"BLIND", "REJECT"})
        private String action;

        @Schema(description = "관리자 메모 (선택)", example = "커뮤니티 가이드라인 위반으로 블라인드 처리")
        private String memo;
    }

    // ==================== User (사용자 관리) ====================

    @Operation(
            summary = "사용자 목록 조회",
            description = "등록된 사용자 목록을 페이지네이션하여 조회합니다. 이메일 또는 닉네임으로 검색할 수 있습니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "관리자 권한 필요",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @GetMapping("/users")
    public ResponseEntity<List<AdminUserDto>> getUsers(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "20")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "검색어 (이메일 또는 닉네임)", example = "홍길동")
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(adminService.getUsers(page, size, search));
    }

    @Operation(
            summary = "사용자 권한 변경",
            description = "사용자의 권한(Role)을 변경합니다. ROLE_USER, ROLE_VERIFIED, ROLE_ADMIN 중 선택 가능합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "변경 완료"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "관리자 권한 필요",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PatchMapping("/users/{id}/role")
    public ResponseEntity<Void> updateUserRole(
            @Parameter(description = "사용자 ID", example = "1", required = true)
            @PathVariable Long id,
            @RequestBody UpdateUserRoleRequest request
    ) {
        adminService.updateUserRole(id, request.getRole());
        return ResponseEntity.ok().build();
    }

    @Schema(description = "사용자 권한 변경 요청")
    @Getter @Setter @NoArgsConstructor
    public static class UpdateUserRoleRequest {
        @Schema(description = "변경할 권한", example = "ROLE_VERIFIED")
        private UserRole role;
    }

    @Operation(
            summary = "사용자 활성화 상태 변경",
            description = "사용자의 활성화 상태를 변경합니다. 비활성화 시 해당 사용자는 로그인할 수 없습니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "변경 완료"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "관리자 권한 필요",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            )
    })
    @PatchMapping("/users/{id}/status")
    public ResponseEntity<Void> updateUserStatus(
            @Parameter(description = "사용자 ID", example = "1", required = true)
            @PathVariable Long id,
            @RequestBody UpdateUserStatusRequest request
    ) {
        adminService.updateUserStatus(id, request.getIsActive());
        return ResponseEntity.ok().build();
    }

    @Schema(description = "사용자 상태 변경 요청")
    @Getter @Setter @NoArgsConstructor
    public static class UpdateUserStatusRequest {
        @Schema(description = "활성화 여부 (true: 활성화, false: 비활성화)", example = "false")
        private Boolean isActive;
    }
}

package com.app.nonstop.domain.verification.controller;

import com.app.nonstop.domain.verification.service.VerificationService;
import com.app.nonstop.global.common.response.ApiResponse;
import com.app.nonstop.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 사용자 인증(Verification) 관련 API 요청을 처리하는 컨트롤러입니다.
 * 학생증 사진 업로드를 통한 대학생 인증 요청 기능을 제공합니다.
 */
@Tag(name = "Verification", description = "사용자 인증 관련 API")
@RestController
@RequestMapping("/api/v1/verification")
@RequiredArgsConstructor
public class VerificationController {

    private final VerificationService verificationService;

    /**
     * 학생증 사진(이미지 파일)을 업로드하여 수동 대학생 인증을 요청합니다.
     * 요청 성공 시 인증 상태는 'PENDING'이 되며, 관리자의 검토를 기다립니다.
     *
     * @param customUserDetails Spring Security에서 주입한 현재 인증된 사용자 정보
     * @param file 업로드할 학생증 이미지 파일 (MultipartFile)
     * @return ApiResponse<?> 성공 응답 (데이터 없음)
     */
    @Operation(
            summary = "학생증 사진 업로드 인증 요청",
            description = "학생증 사진(이미지 파일)을 업로드하여 수동 대학생 인증을 요청합니다. 요청 성공 시 인증 상태는 'PENDING'이 됩니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping(value = "/student-id", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<?>> requestStudentIdVerification(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @Parameter(description = "업로드할 학생증 이미지 파일")
            @RequestParam("file") MultipartFile file
    ) {
        verificationService.requestStudentIdVerification(customUserDetails.getUserId(), file);
        return ResponseEntity.ok(ApiResponse.success());
    }
}

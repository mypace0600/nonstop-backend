package com.app.nonstop.domain.user.controller;

import com.app.nonstop.domain.user.dto.PasswordUpdateRequestDto;
import com.app.nonstop.domain.user.dto.ProfileUpdateRequestDto;
import com.app.nonstop.domain.user.dto.UserResponseDto;
import com.app.nonstop.domain.user.dto.VerificationStatusResponseDto;
import com.app.nonstop.domain.user.service.UserService;
import com.app.nonstop.global.common.response.ApiResponse;
import com.app.nonstop.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "사용자 정보 관련 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 정보 조회", description = "현재 로그인된 사용자의 정보를 조회합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDto>> getMyInfo(
            @Parameter(hidden = true) // Swagger 문서에서 파라미터 입력을 숨기기 위한 어노테이션
            @AuthenticationPrincipal CustomUserDetails customUserDetails
            /**
             * @AuthenticationPrincipal: Spring Security의 SecurityContextHolder에서 현재 인증된 사용자의 Principal 객체를 주입받습니다.
             * JwtAuthenticationFilter에서 인증 성공 시 SecurityContext에 CustomUserDetails를 담아두었기 때문에,
             * 컨트롤러에서는 이 어노테이션 하나로 편리하게 사용자 정보(ID 등)를 얻을 수 있습니다.
             */
    ) {
        UserResponseDto myInfo = userService.getMyInfo(customUserDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(myInfo));
    }

    @Operation(summary = "내 정보 수정", description = "현재 로그인된 사용자의 프로필 정보(닉네임, 학교, 전공 등)를 수정합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<?>> updateMyProfile(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody @Valid ProfileUpdateRequestDto requestDto
    ) {
        userService.updateProfile(customUserDetails.getUserId(), requestDto);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "비밀번호 변경", description = "현재 로그인된 사용자의 비밀번호를 변경합니다. 이메일 가입자만 가능합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<?>> updateMyPassword(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody @Valid PasswordUpdateRequestDto requestDto
    ) {
        userService.updatePassword(customUserDetails.getUserId(), requestDto);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "회원 탈퇴", description = "현재 로그인된 사용자의 계정을 비활성화(soft delete)합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<?>> deactivateMyAccount(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        userService.deactivateAccount(customUserDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "인증 상태 조회", description = "현재 로그인된 사용자의 대학생 인증 상태 및 인증 방식을 조회합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me/verification-status")
    public ResponseEntity<ApiResponse<VerificationStatusResponseDto>> getMyVerificationStatus(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        VerificationStatusResponseDto responseDto = userService.getVerificationStatus(customUserDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(responseDto));
    }
}

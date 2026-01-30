package com.app.nonstop.domain.auth.controller;

import com.app.nonstop.domain.auth.dto.*;
import com.app.nonstop.domain.auth.service.AuthService;
import com.app.nonstop.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "인증 및 토큰 관리 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "이메일 회원가입", description = "이메일, 비밀번호, 닉네임, 생년월일로 회원가입을 진행합니다. 가입 전 이메일 인증이 완료되어야 합니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignUpResponseDto>> signUp(@RequestBody @Valid SignUpRequestDto request) {
        SignUpResponseDto response = authService.signUp(request);
        return ResponseEntity.status(201).body(ApiResponse.success(response));
    }

    @Operation(summary = "이메일 인증 코드 요청", description = "이메일 인증 코드를 발송합니다. (1분 제한)")
    @PostMapping("/email/send-verification")
    public ResponseEntity<ApiResponse<?>> sendEmailVerification(@RequestBody @Valid EmailVerificationRequestDto request) {
        authService.sendEmailVerification(request);
        return ResponseEntity.ok(ApiResponse.success("인증 메일이 발송되었습니다."));
    }

    @Operation(summary = "이메일 인증 확인", description = "이메일로 전송된 인증 코드를 확인합니다. 성공 시 해당 이메일은 가입 가능한 상태가 됩니다.")
    @PostMapping("/email/verify")
    public ResponseEntity<ApiResponse<?>> verifyEmail(@RequestBody @Valid SignupVerificationRequestDto request) {
        authService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.success("이메일 인증이 완료되었습니다."));
    }

    @Operation(summary = "이메일 로그인", description = "이메일과 비밀번호로 로그인을 진행하고 토큰을 발급합니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponseDto>> login(@RequestBody @Valid LoginRequestDto request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        TokenResponseDto tokenResponse = authService.login(request, ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.success(tokenResponse));
    }

    @Operation(summary = "Google 로그인", description = "Google ID 토큰으로 로그인 또는 회원가입을 진행합니다.")
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<TokenResponseDto>> googleLogin(@RequestBody @Valid GoogleLoginRequestDto request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        TokenResponseDto tokenResponse = authService.googleLogin(request, ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.success(tokenResponse));
    }

    @Operation(summary = "로그아웃", description = "사용자의 Refresh Token을 만료시켜 로그아웃 처리합니다.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<?>> logout(@RequestBody @Valid RefreshRequestDto request, HttpServletRequest httpRequest) {
        String ipAddress = getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        authService.logout(request.getRefreshToken(), ipAddress, userAgent);
        return ResponseEntity.ok(ApiResponse.success());
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    @Operation(summary = "Access Token 재발급", description = "유효한 Refresh Token으로 새로운 Access Token과 Refresh Token을 발급합니다.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponseDto>> refresh(@RequestBody @Valid RefreshRequestDto request) {
        TokenResponseDto tokenResponse = authService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(tokenResponse));
    }

    @Operation(summary = "이메일 중복 확인", description = "회원가입 시 이메일 중복 여부를 확인합니다.")
    @PostMapping("/email/check")
    public ResponseEntity<ApiResponse<?>> checkEmailDuplicate(@RequestBody @Valid EmailCheckRequestDto request) {
        authService.checkEmailDuplicate(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "닉네임 중복 확인", description = "회원가입 또는 프로필 수정 시 닉네임 중복 여부를 확인합니다.")
    @PostMapping("/nickname/check")
    public ResponseEntity<ApiResponse<?>> checkNicknameDuplicate(@RequestBody @Valid NicknameCheckRequestDto request) {
        authService.checkNicknameDuplicate(request.getNickname());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "비밀번호 재설정 요청", description = "비밀번호 재설정을 위한 인증 코드를 이메일로 발송합니다.")
    @PostMapping("/password/reset/request")
    public ResponseEntity<ApiResponse<?>> requestPasswordReset(@RequestBody @Valid PasswordResetRequestDto request) {
        authService.sendPasswordResetCode(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success("비밀번호 재설정 코드가 발송되었습니다."));
    }

    @Operation(summary = "비밀번호 재설정 코드 확인", description = "이메일로 전송된 비밀번호 재설정 코드를 확인합니다.")
    @PostMapping("/password/reset/verify")
    public ResponseEntity<ApiResponse<?>> verifyPasswordResetCode(@RequestBody @Valid PasswordResetVerifyDto request) {
        authService.verifyPasswordResetCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok(ApiResponse.success("인증 코드가 확인되었습니다."));
    }

    @Operation(summary = "비밀번호 재설정 확정", description = "인증된 코드를 사용하여 새 비밀번호를 설정합니다.")
    @PostMapping("/password/reset/confirm")
    public ResponseEntity<ApiResponse<?>> confirmPasswordReset(@RequestBody @Valid PasswordResetConfirmDto request) {
        authService.confirmPasswordReset(request.getEmail(), request.getCode(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("비밀번호가 성공적으로 변경되었습니다."));
    }
}

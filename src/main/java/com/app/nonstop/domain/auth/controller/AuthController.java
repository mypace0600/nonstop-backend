package com.app.nonstop.domain.auth.controller;

import com.app.nonstop.domain.auth.dto.AuthDto;
import com.app.nonstop.domain.auth.service.AuthService;
import com.app.nonstop.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

    @Operation(summary = "이메일 회원가입", description = "이메일, 비밀번호, 닉네임으로 회원가입을 진행합니다.")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<?>> signUp(@RequestBody @Valid AuthDto.SignUpRequest request) {
        authService.signUp(request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "이메일 로그인", description = "이메일과 비밀번호로 로그인을 진행하고 토큰을 발급합니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDto.TokenResponse>> login(@RequestBody @Valid AuthDto.LoginRequest request) {
        AuthDto.TokenResponse tokenResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(tokenResponse));
    }

    @Operation(summary = "Google 로그인", description = "Google ID 토큰으로 로그인 또는 회원가입을 진행합니다.")
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthDto.TokenResponse>> googleLogin(@RequestBody @Valid AuthDto.GoogleLoginRequest request) {
        AuthDto.TokenResponse tokenResponse = authService.googleLogin(request);
        return ResponseEntity.ok(ApiResponse.success(tokenResponse));
    }

    @Operation(summary = "로그아웃", description = "사용자의 Refresh Token을 만료시켜 로그아웃 처리합니다.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<?>> logout(@RequestBody @Valid AuthDto.RefreshRequest request) {
        authService.logout(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "Access Token 재발급", description = "유효한 Refresh Token으로 새로운 Access Token과 Refresh Token을 발급합니다.")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthDto.TokenResponse>> refresh(@RequestBody @Valid AuthDto.RefreshRequest request) {
        AuthDto.TokenResponse tokenResponse = authService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.success(tokenResponse));
    }

    @Operation(summary = "이메일 중복 확인", description = "회원가입 시 이메일 중복 여부를 확인합니다.")
    @PostMapping("/email/check")
    public ResponseEntity<ApiResponse<?>> checkEmailDuplicate(@RequestBody @Valid AuthDto.EmailCheckRequest request) {
        authService.checkEmailDuplicate(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "닉네임 중복 확인", description = "회원가입 또는 프로필 수정 시 닉네임 중복 여부를 확인합니다.")
    @PostMapping("/nickname/check")
    public ResponseEntity<ApiResponse<?>> checkNicknameDuplicate(@RequestBody @Valid AuthDto.NicknameCheckRequest request) {
        authService.checkNicknameDuplicate(request.getNickname());
        return ResponseEntity.ok(ApiResponse.success());
    }
}

package com.app.nonstop.domain.auth.service;

import com.app.nonstop.domain.auth.dto.*;

public interface AuthService {
    SignUpResponseDto signUp(SignUpRequestDto signUpRequest);

    TokenResponseDto login(LoginRequestDto loginRequest, String ipAddress, String userAgent);

    TokenResponseDto googleLogin(GoogleLoginRequestDto googleLoginRequest, String ipAddress, String userAgent);

    TokenResponseDto appleLogin(AppleLoginRequestDto appleLoginRequest, String ipAddress, String userAgent);

    void logout(String refreshToken, String ipAddress, String userAgent);

    TokenResponseDto refresh(String refreshTokenValue);

    void checkEmailDuplicate(String email);

    void checkNicknameDuplicate(String nickname);

    void sendEmailVerification(EmailVerificationRequestDto request);

    void verifyEmail(SignupVerificationRequestDto request);

    void cleanupUnverifiedUsers();

    void sendPasswordResetCode(String email);

    void verifyPasswordResetCode(String email, String code);

    void confirmPasswordReset(String email, String code, String newPassword);
}

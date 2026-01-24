package com.app.nonstop.domain.auth.service;

import com.app.nonstop.domain.auth.dto.*;

public interface AuthService {
    void signUp(SignUpRequestDto signUpRequest);

    TokenResponseDto login(LoginRequestDto loginRequest, String ipAddress, String userAgent);

    TokenResponseDto googleLogin(GoogleLoginRequestDto googleLoginRequest, String ipAddress, String userAgent);

    void logout(String refreshToken, String ipAddress, String userAgent);

    TokenResponseDto refresh(String refreshTokenValue);

    void checkEmailDuplicate(String email);

    void checkNicknameDuplicate(String nickname);

    TokenResponseDto verifySignupEmail(SignupVerificationRequestDto request);

    void resendSignupVerificationCode(SignupResendRequestDto request);

    void cleanupUnverifiedUsers();
}

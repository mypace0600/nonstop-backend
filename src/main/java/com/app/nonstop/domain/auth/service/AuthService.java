package com.app.nonstop.domain.auth.service;

import com.app.nonstop.domain.auth.dto.*;

public interface AuthService {
    void signUp(SignUpRequestDto signUpRequest);

    TokenResponseDto login(LoginRequestDto loginRequest);

    TokenResponseDto googleLogin(GoogleLoginRequestDto googleLoginRequest);

    void logout(String refreshToken);

    TokenResponseDto refresh(String refreshTokenValue);

    void checkEmailDuplicate(String email);

    void checkNicknameDuplicate(String nickname);
}

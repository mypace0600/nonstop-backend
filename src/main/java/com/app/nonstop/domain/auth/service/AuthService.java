package com.app.nonstop.domain.auth.service;

import com.app.nonstop.domain.auth.dto.AuthDto;

public interface AuthService {
    void signUp(AuthDto.SignUpRequest signUpRequest);

    AuthDto.TokenResponse login(AuthDto.LoginRequest loginRequest);

    AuthDto.TokenResponse googleLogin(AuthDto.GoogleLoginRequest googleLoginRequest);

    void logout(String refreshToken);

    AuthDto.TokenResponse refresh(String refreshTokenValue);

    void checkEmailDuplicate(String email);

    void checkNicknameDuplicate(String nickname);
}

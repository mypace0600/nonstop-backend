package com.app.nonstop.domain.auth.service;

import com.app.nonstop.domain.auth.dto.AuthDto;
import com.app.nonstop.domain.auth.mapper.AuthMapper;
import com.app.nonstop.domain.token.entity.RefreshToken;
import com.app.nonstop.domain.token.mapper.RefreshTokenMapper;
import com.app.nonstop.domain.user.entity.AuthProvider;
import com.app.nonstop.domain.user.entity.User;
import com.app.nonstop.domain.user.exception.DuplicateNicknameException;
import com.app.nonstop.domain.user.exception.InvalidPasswordException;
import com.app.nonstop.domain.user.exception.UserNotFoundException;
import com.app.nonstop.global.security.jwt.JwtTokenProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final AuthMapper authMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final FirebaseAuth firebaseAuth;

    @Override
    public void signUp(AuthDto.SignUpRequest signUpRequest) {
        checkEmailDuplicate(signUpRequest.getEmail());
        checkNicknameDuplicate(signUpRequest.getNickname());

        User user = signUpRequest.toEntity(passwordEncoder);
        authMapper.save(user);
    }

    @Override
    public AuthDto.TokenResponse login(AuthDto.LoginRequest loginRequest) {
        User user = authMapper.findByEmail(loginRequest.getEmail())
                .orElseThrow(UserNotFoundException::new);

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new InvalidPasswordException();
        }

        return issueTokens(user);
    }

    @Override
    public AuthDto.TokenResponse googleLogin(AuthDto.GoogleLoginRequest googleLoginRequest) {
        FirebaseToken firebaseToken;
        try {
            firebaseToken = firebaseAuth.verifyIdToken(googleLoginRequest.getIdToken());
        } catch (Exception e) {
            throw new RuntimeException("Invalid Google ID token.");
        }

        String email = firebaseToken.getEmail();
        User user = authMapper.findByEmail(email)
                .orElseGet(() -> {
                    String nickname = firebaseToken.getName();
                    if (authMapper.existsByNickname(nickname)) {
                        nickname = nickname + UUID.randomUUID().toString().substring(0, 4);
                    }
                    User newUser = User.builder()
                            .email(email)
                            .nickname(nickname)
                            .authProvider(AuthProvider.GOOGLE)
                            .profileImageUrl(firebaseToken.getPicture())
                            .build();
                    authMapper.save(newUser);
                    return newUser;
                });

        return issueTokens(user);
    }


    private AuthDto.TokenResponse issueTokens(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user);
        String refreshToken = jwtTokenProvider.createRefreshToken(user);

        refreshTokenMapper.deleteByUserId(user.getId());

        RefreshToken newRefreshToken = RefreshToken.builder()
                .userId(user.getId())
                .token(refreshToken)
                .expiresAt(Instant.now().plus(jwtTokenProvider.getRefreshTokenValidity()))
                .build();
        refreshTokenMapper.save(newRefreshToken);

        return AuthDto.TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }


    @Override
    public void logout(String refreshToken) {
        RefreshToken token = refreshTokenMapper.findByToken(refreshToken)
                .orElse(null);
        if (token != null) {
            refreshTokenMapper.deleteByUserId(token.getUserId());
        }
    }

    @Override
    public AuthDto.TokenResponse refresh(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenMapper.findByToken(refreshTokenValue)
                .orElseThrow(() -> new RuntimeException("Refresh Token not found"));

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Refresh Token expired");
        }

        User user = authMapper.findByEmail(jwtTokenProvider.getUserEmail(refreshToken.getToken()))
                .orElseThrow(UserNotFoundException::new);


        return issueTokens(user);
    }

    @Override
    public void checkEmailDuplicate(String email) {
        if (authMapper.existsByEmail(email)) {
            throw new RuntimeException("이미 사용중인 이메일입니다.");
        }
    }

    @Override
    public void checkNicknameDuplicate(String nickname) {
        if (authMapper.existsByNickname(nickname)) {
            throw new DuplicateNicknameException();
        }
    }
}

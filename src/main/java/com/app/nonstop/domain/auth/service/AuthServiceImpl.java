package com.app.nonstop.domain.auth.service;

import com.app.nonstop.domain.auth.dto.*;
import com.app.nonstop.domain.token.entity.RefreshToken;
import com.app.nonstop.mapper.AuthMapper;
import com.app.nonstop.mapper.RefreshTokenMapper;
import com.app.nonstop.domain.user.entity.AuthProvider;
import com.app.nonstop.domain.user.entity.User;
import com.app.nonstop.domain.user.exception.DuplicateNicknameException;
import com.app.nonstop.domain.user.exception.InvalidPasswordException;
import com.app.nonstop.domain.user.exception.UserNotFoundException;
import com.app.nonstop.global.security.jwt.JwtTokenProvider;
import com.app.nonstop.global.security.user.CustomUserDetails;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;


@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final AuthMapper authMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final Optional<FirebaseAuth> firebaseAuth;

    @Autowired
    public AuthServiceImpl(AuthMapper authMapper, RefreshTokenMapper refreshTokenMapper, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider, Optional<FirebaseAuth> firebaseAuth) {
        this.authMapper = authMapper;
        this.refreshTokenMapper = refreshTokenMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.firebaseAuth = firebaseAuth;
    }


    @Override
    public void signUp(SignUpRequestDto signUpRequest) {
        checkEmailDuplicate(signUpRequest.getEmail());
        checkNicknameDuplicate(signUpRequest.getNickname());

        User user = signUpRequest.toEntity(passwordEncoder);
        authMapper.save(user);
    }

    @Override
    public TokenResponseDto login(LoginRequestDto loginRequest) {
        User user = authMapper.findByEmail(loginRequest.getEmail())
                .orElseThrow(UserNotFoundException::new);

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new InvalidPasswordException();
        }

        return issueTokens(user);
    }

    @Override
    public TokenResponseDto googleLogin(GoogleLoginRequestDto googleLoginRequest) {
        FirebaseAuth auth = firebaseAuth.orElseThrow(() -> new IllegalStateException("Firebase not configured for this environment."));
        FirebaseToken firebaseToken;
        try {
            firebaseToken = auth.verifyIdToken(googleLoginRequest.getIdToken());
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


    private TokenResponseDto issueTokens(User user) {
        CustomUserDetails userDetails = new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getUniversityId(),
                user.getIsVerified(),
                Collections.singletonList(new SimpleGrantedAuthority(user.getUserRole().name()))
        );
        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        String accessToken = jwtTokenProvider.createAccessToken(authentication);
        String refreshToken = jwtTokenProvider.createRefreshToken(authentication);

        refreshTokenMapper.deleteByUserId(user.getId());

        RefreshToken newRefreshToken = RefreshToken.builder()
                .userId(user.getId())
                .token(refreshToken)
                .expiresAt(Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenValidity()))
                .build();
        refreshTokenMapper.save(newRefreshToken);

        return TokenResponseDto.builder()
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
    public TokenResponseDto refresh(String refreshTokenValue) {
        if (!jwtTokenProvider.validateToken(refreshTokenValue)) {
            throw new RuntimeException("Invalid Refresh Token");
        }

        RefreshToken refreshToken = refreshTokenMapper.findByToken(refreshTokenValue)
                .orElseThrow(() -> new RuntimeException("Refresh Token not found in DB"));

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new RuntimeException("Refresh Token expired");
        }

        String email = jwtTokenProvider.getEmailFromToken(refreshToken.getToken());
        User user = authMapper.findByEmail(email)
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


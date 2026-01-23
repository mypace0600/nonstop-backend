package com.app.nonstop.domain.auth.service;

import com.app.nonstop.domain.auth.dto.*;
import com.app.nonstop.domain.auth.exception.*;
import com.app.nonstop.domain.policy.dto.PolicyResponseDto;
import com.app.nonstop.domain.policy.service.PolicyService;
import com.app.nonstop.domain.token.entity.RefreshToken;
import com.app.nonstop.mapper.AuthMapper;
import com.app.nonstop.mapper.RefreshTokenMapper;
import com.app.nonstop.mapper.UserMapper;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final AuthMapper authMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final Optional<FirebaseAuth> firebaseAuth;
    private final PolicyService policyService;

    @Autowired
    public AuthServiceImpl(AuthMapper authMapper, RefreshTokenMapper refreshTokenMapper, UserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider, Optional<FirebaseAuth> firebaseAuth, PolicyService policyService) {
        this.authMapper = authMapper;
        this.refreshTokenMapper = refreshTokenMapper;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.firebaseAuth = firebaseAuth;
        this.policyService = policyService;
    }


    @Override
    public void signUp(SignUpRequestDto signUpRequest) {
        checkEmailDuplicate(signUpRequest.getEmail());
        checkNicknameDuplicate(signUpRequest.getNickname());

        User user = signUpRequest.toEntity(passwordEncoder);
        authMapper.save(user);

        // 정책 동의 저장
        policyService.agreePolicies(user.getId(), signUpRequest.getAgreedPolicyIds());
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
            throw new InvalidTokenException("유효하지 않은 Google ID 토큰입니다.");
        }

        String email = firebaseToken.getEmail();
        String googleProfileImage = firebaseToken.getPicture();

        User user = authMapper.findByEmail(email)
                .map(existingUser -> {
                    // 기존 사용자: Google 프로필 이미지가 변경되었으면 업데이트
                    if (googleProfileImage != null && !googleProfileImage.equals(existingUser.getProfileImageUrl())) {
                        userMapper.updateProfileImage(existingUser.getId(), googleProfileImage);
                    }
                    return existingUser;
                })
                .orElseGet(() -> {
                    // 신규 사용자: 회원가입 처리
                    String nickname = firebaseToken.getName();
                    if (authMapper.existsByNickname(nickname)) {
                        nickname = nickname + UUID.randomUUID().toString().substring(0, 4);
                    }
                    User newUser = User.builder()
                            .email(email)
                            .nickname(nickname)
                            .authProvider(AuthProvider.GOOGLE)
                            .profileImageUrl(googleProfileImage)
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

        // 기존 Refresh Token soft-delete (revoke)
        refreshTokenMapper.revokeByUserId(user.getId());

        RefreshToken newRefreshToken = RefreshToken.builder()
                .userId(user.getId())
                .token(refreshToken)
                .expiresAt(Instant.now().plusMillis(jwtTokenProvider.getRefreshTokenValidity()))
                .build();
        refreshTokenMapper.save(newRefreshToken);

        return TokenResponseDto.builder()
                .userId(user.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }


    @Override
    public void logout(String refreshToken) {
        RefreshToken token = refreshTokenMapper.findByToken(refreshToken)
                .orElse(null);
        if (token != null) {
            // Refresh Token soft-delete (revoke) - 토큰 사용 기록 추적 가능
            refreshTokenMapper.revokeByUserId(token.getUserId());
        }
    }

    @Override
    public TokenResponseDto refresh(String refreshTokenValue) {
        if (!jwtTokenProvider.validateToken(refreshTokenValue)) {
            throw new InvalidTokenException("유효하지 않은 Refresh Token입니다.");
        }

        RefreshToken refreshToken = refreshTokenMapper.findByToken(refreshTokenValue)
                .orElseThrow(TokenNotFoundException::new);

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ExpiredTokenException("만료된 Refresh Token입니다.");
        }

        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken.getToken());
        User user = userMapper.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        return issueTokens(user);
    }

    @Override
    public void checkEmailDuplicate(String email) {
        if (authMapper.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }
    }

    @Override
    public void checkNicknameDuplicate(String nickname) {
        if (authMapper.existsByNickname(nickname)) {
            throw new DuplicateNicknameException();
        }
    }
}


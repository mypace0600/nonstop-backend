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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


import com.app.nonstop.global.util.EmailService;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.LocalDate;
import java.time.Period;
import java.util.Random;
import java.util.concurrent.TimeUnit;


@Slf4j
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
    private final EmailService emailService;
    private final StringRedisTemplate redisTemplate;

    private static final String SIGNUP_VERIFICATION_PREFIX = "signup:verification:";
    private static final String SIGNUP_RESEND_LIMIT_PREFIX = "signup:resend:limit:";
    private static final long SIGNUP_VERIFICATION_TTL = 5; // 5분
    private static final long SIGNUP_RESEND_LIMIT_TTL = 1; // 1분

    @Autowired
    public AuthServiceImpl(AuthMapper authMapper, RefreshTokenMapper refreshTokenMapper, UserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider, Optional<FirebaseAuth> firebaseAuth, PolicyService policyService, EmailService emailService, StringRedisTemplate redisTemplate) {
        this.authMapper = authMapper;
        this.refreshTokenMapper = refreshTokenMapper;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.firebaseAuth = firebaseAuth;
        this.policyService = policyService;
        this.emailService = emailService;
        this.redisTemplate = redisTemplate;
    }


    @Override
    public SignUpResponseDto signUp(SignUpRequestDto signUpRequest) {
        checkEmailDuplicate(signUpRequest.getEmail());
        checkNicknameDuplicate(signUpRequest.getNickname());

        // 만 14세 미만 체크
        validateAge(signUpRequest.getBirthDate());

        User user = signUpRequest.toEntity(passwordEncoder);
        authMapper.save(user);

        // 정책 동의 저장
        policyService.agreePolicies(user.getId(), signUpRequest.getAgreedPolicyIds());

        // 이메일 인증은 별도 API에서 처리
        return new SignUpResponseDto(user.getId(), user.getEmail());
    }

    @Override
    public void sendEmailVerification(EmailVerificationRequestDto request) {
        String email = request.getEmail();

        // Rate Limit 체크
        String rateLimitKey = SIGNUP_RESEND_LIMIT_PREFIX + email;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(rateLimitKey))) {
            throw new ResendRateLimitedException();
        }

        User user = authMapper.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new AlreadyVerifiedException();
        }

        // 인증 코드 발송
        sendVerificationEmail(email);

        // Rate Limit 설정
        redisTemplate.opsForValue().set(rateLimitKey, "1", SIGNUP_RESEND_LIMIT_TTL, TimeUnit.MINUTES);
    }

    private void validateAge(LocalDate birthDate) {
        if (birthDate == null) return;
        if (Period.between(birthDate, LocalDate.now()).getYears() < 14) {
            throw new UnderAgeException();
        }
    }

    private void sendVerificationEmail(String email) {
        // 6자리 난수 생성
        String code = String.valueOf(new Random().nextInt(900000) + 100000);

        // Redis 저장
        redisTemplate.opsForValue().set(SIGNUP_VERIFICATION_PREFIX + email, code, SIGNUP_VERIFICATION_TTL, TimeUnit.MINUTES);

        // 이메일 발송
        emailService.sendSimpleMessage(email, "[Nonstop] 회원가입 인증 코드", 
                "회원가입을 위해 아래 인증 코드를 입력해주세요.\n\n인증 코드: " + code + "\n\n5분 내에 입력해주세요.");
    }

    @Override
    public TokenResponseDto verifyEmail(SignupVerificationRequestDto request) {
        String email = request.getEmail();
        String code = request.getCode();
        String redisKey = SIGNUP_VERIFICATION_PREFIX + email;

        String storedCode = redisTemplate.opsForValue().get(redisKey);

        if (storedCode == null) {
            throw new VerificationCodeExpiredException();
        }

        if (!storedCode.equals(code)) {
            throw new VerificationCodeMismatchException();
        }

        User user = authMapper.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);

        // 이미 인증된 경우 처리 (선택적)
        if (Boolean.TRUE.equals(user.getEmailVerified())) {
             // 이미 인증되었으면 바로 토큰 발급
             return issueTokens(user);
        }

        // DB 업데이트
        authMapper.updateEmailVerified(user.getId(), true, LocalDateTime.now());

        // 변경된 상태 반영
        User updatedUser = User.builder()
                .id(user.getId())
                .email(user.getEmail())
                .userRole(user.getUserRole())
                .universityId(user.getUniversityId())
                .isVerified(user.getIsVerified())
                .emailVerified(true)
                .birthDate(user.getBirthDate())
                .build();

        // Redis 키 삭제
        redisTemplate.delete(redisKey);

        return issueTokens(updatedUser);
    }

    @Override
    public TokenResponseDto login(LoginRequestDto loginRequest, String ipAddress, String userAgent) {
        User user = authMapper.findByEmail(loginRequest.getEmail())
                .orElseThrow(UserNotFoundException::new);

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            throw new InvalidPasswordException();
        }

        return issueTokens(user);
    }

    @Override
    public TokenResponseDto googleLogin(GoogleLoginRequestDto googleLoginRequest, String ipAddress, String userAgent) {
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

        // 필수 정책 동의 여부 확인
        boolean hasAgreedAllMandatory = policyService.getMissingMandatoryPolicies(user.getId()).isEmpty();

        return TokenResponseDto.builder()
                .userId(user.getId())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .emailVerified(user.getEmailVerified())
                .hasAgreedAllMandatory(hasAgreedAllMandatory)
                .hasBirthDate(user.getBirthDate() != null)
                .build();
    }


    @Override
    public void logout(String refreshToken, String ipAddress, String userAgent) {
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

    @Override
    public void cleanupUnverifiedUsers() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);
        int deletedCount = authMapper.deleteUnverifiedUsersBefore(threshold);
        log.info("Cleaned up {} unverified users before {}", deletedCount, threshold);
    }
}


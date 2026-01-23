package com.app.nonstop.global.config;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class SecurityPathConfig {

    // 1. JWT 필터 및 Security 상에서 권한 없이 접근 가능한 경로 (Public)
    public static final String[] PUBLIC_URLS = {
            "/api/v1/auth/login",
            "/api/v1/auth/signup",
            "/api/v1/auth/google",
            "/api/v1/auth/email/check",
            "/api/v1/auth/nickname/check",
            "/api/v1/universities/list",
            "/api/v1/universities/regions",
            "/oauth2/**",
            "/api-docs/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    // 2. 정책 동의 필터(PolicyAgreementFilter)에서 검증을 제외할 경로
    // (동의를 하기 위해 필요한 API + Public 경로 + 세션 유지를 위한 최소한의 Auth API)
    public static final String[] POLICY_EXCLUDE_URLS = {
            "/api/v1/policies/**",          // 정책 조회 및 동의 (필수)
            "/api/v1/auth/login",
            "/api/v1/auth/signup",
            "/api/v1/auth/google",
            "/api/v1/auth/email/check",
            "/api/v1/auth/nickname/check",
            "/api/v1/auth/logout",           // 정책 미동의 상태에서도 로그아웃은 허용
            "/api/v1/auth/refresh",          // 정책 미동의 상태에서도 세션 갱신은 허용
            "/api/v1/universities/list",
            "/api/v1/universities/regions",
            "/oauth2/**",
            "/api-docs/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    public boolean isPublic(String path, org.springframework.util.AntPathMatcher pathMatcher) {
        return Arrays.stream(PUBLIC_URLS).anyMatch(p -> pathMatcher.match(p, path));
    }

    public boolean isPolicyExcluded(String path, org.springframework.util.AntPathMatcher pathMatcher) {
        return Arrays.stream(POLICY_EXCLUDE_URLS).anyMatch(p -> pathMatcher.match(p, path));
    }
}

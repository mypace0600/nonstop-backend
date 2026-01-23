package com.app.nonstop.global.config;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class SecurityPathConfig {

    // 1. JWT 필터 및 Security 상에서 권한 없이 접근 가능한 경로 (Public)
    public static final String[] PUBLIC_URLS = {
            "/api/v1/auth/**",
            "/api/v1/universities/list",
            "/api/v1/universities/regions",
            "/oauth2/**",
            "/api-docs/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    // 2. 정책 동의 필터(PolicyAgreementFilter)에서 검증을 제외할 경로
    // (Public 경로 + 정책 관련 API)
    public static final String[] POLICY_EXCLUDE_URLS = {
            "/api/v1/auth/**",
            "/api/v1/policies/**",
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

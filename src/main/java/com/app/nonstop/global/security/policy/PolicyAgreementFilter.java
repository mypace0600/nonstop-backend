package com.app.nonstop.global.security.policy;

import com.app.nonstop.domain.policy.service.PolicyService;
import com.app.nonstop.global.config.SecurityPathConfig;
import com.app.nonstop.global.security.user.CustomUserDetails;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class PolicyAgreementFilter extends OncePerRequestFilter {

    private final PolicyService policyService;
    private final SecurityPathConfig securityPathConfig;
    private final org.springframework.util.AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // 1. 제외 경로 체크 (SecurityPathConfig 통합 관리)
        if (securityPathConfig.isPolicyExcluded(path, pathMatcher)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. 인증 여부 확인
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            
            // 3. 필수 정책 동의 여부 확인
            // TODO: 성능 최적화를 위해 이 정보를 UserDetails에 포함하거나 캐싱하는 것을 고려할 수 있음
            boolean hasMissingMandatory = !policyService.getMissingMandatoryPolicies(userDetails.getUserId()).isEmpty();

            if (hasMissingMandatory) {
                sendErrorResponse(response);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"success\":false,\"message\":\"필수 약관 동의가 필요합니다.\",\"code\":\"POLICY_AGREEMENT_REQUIRED\"}");
    }
}

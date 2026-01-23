package com.app.nonstop.domain.policy.controller;

import com.app.nonstop.domain.policy.dto.PolicyAgreeRequestDto;
import com.app.nonstop.domain.policy.dto.PolicyResponseDto;
import com.app.nonstop.domain.policy.dto.UserPolicyAgreementDto;
import com.app.nonstop.domain.policy.service.PolicyService;
import com.app.nonstop.global.common.response.ApiResponse;
import com.app.nonstop.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
@Tag(name = "Policy", description = "정책 및 약관 API")
public class PolicyController {

    private final PolicyService policyService;

    @GetMapping
    @Operation(summary = "정책 목록 조회", description = "활성화된 모든 정책 목록을 조회합니다.")
    public List<PolicyResponseDto> getPolicies() {
        return policyService.getAllActivePolicies();
    }

    @GetMapping("/me")
    @Operation(summary = "내 동의 내역 조회", description = "로그인한 사용자가 동의한 정책 목록을 조회합니다.")
    public List<UserPolicyAgreementDto> getMyAgreements(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return policyService.getUserAgreements(userDetails.getUserId());
    }

    @PostMapping("/agree")
    @Operation(summary = "정책 동의 처리", description = "로그인한 사용자가 새로운 정책에 동의합니다.")
    public ApiResponse<?> agreePolicies(@AuthenticationPrincipal CustomUserDetails userDetails, @RequestBody PolicyAgreeRequestDto requestDto) {
        policyService.agreePolicies(userDetails.getUserId(), requestDto.getPolicyIds());
        return ApiResponse.success();
    }
}

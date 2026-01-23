package com.app.nonstop.domain.auth.exception;

import com.app.nonstop.domain.policy.dto.PolicyResponseDto;
import lombok.Getter;

import java.util.List;

@Getter
public class PolicyAgreementRequiredException extends RuntimeException {
    private final List<PolicyResponseDto> requiredPolicies;

    public PolicyAgreementRequiredException(List<PolicyResponseDto> requiredPolicies) {
        super("필수 약관 동의가 필요합니다.");
        this.requiredPolicies = requiredPolicies;
    }
}

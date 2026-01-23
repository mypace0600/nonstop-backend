package com.app.nonstop.domain.policy.service;

import com.app.nonstop.domain.policy.dto.PolicyResponseDto;
import com.app.nonstop.domain.policy.dto.UserPolicyAgreementDto;

import java.util.List;

public interface PolicyService {
    List<PolicyResponseDto> getAllActivePolicies();
    List<UserPolicyAgreementDto> getUserAgreements(Long userId);
    void agreePolicies(Long userId, List<Long> policyIds);
    List<PolicyResponseDto> getMissingMandatoryPolicies(Long userId);
}

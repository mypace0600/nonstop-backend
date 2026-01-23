package com.app.nonstop.domain.policy.service;

import com.app.nonstop.domain.policy.dto.PolicyResponseDto;
import com.app.nonstop.domain.policy.dto.UserPolicyAgreementDto;
import com.app.nonstop.domain.policy.entity.Policy;
import com.app.nonstop.domain.policy.entity.UserPolicyAgreement;
import com.app.nonstop.global.common.exception.BusinessException;
import com.app.nonstop.mapper.PolicyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyServiceImpl implements PolicyService {

    private final PolicyMapper policyMapper;

    @Override
    public List<PolicyResponseDto> getAllActivePolicies() {
        return policyMapper.findAllActive().stream()
                .map(PolicyResponseDto::from)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserPolicyAgreementDto> getUserAgreements(Long userId) {
        return policyMapper.findAgreementsByUserId(userId);
    }

    @Override
    @Transactional
    public void agreePolicies(Long userId, List<Long> policyIds) {
        if (policyIds == null || policyIds.isEmpty()) {
            return;
        }
        
        List<UserPolicyAgreement> agreements = policyIds.stream()
                .map(policyId -> UserPolicyAgreement.builder()
                        .userId(userId)
                        .policyId(policyId)
                        .build())
                .collect(Collectors.toList());

        policyMapper.saveAgreements(agreements);
    }

    @Override
    public List<PolicyResponseDto> getMissingMandatoryPolicies(Long userId) {
        // 1. 활성화된 필수 정책 목록 조회
        List<Policy> mandatoryPolicies = policyMapper.findAllActive().stream()
                .filter(Policy::getIsMandatory)
                .collect(Collectors.toList());

        // 2. 사용자가 동의한 정책 ID 목록 조회
        List<Long> agreedPolicyIds = policyMapper.findAgreementsByUserId(userId).stream()
                .map(UserPolicyAgreementDto::getPolicyId)
                .collect(Collectors.toList());

        // 3. 동의하지 않은 필수 정책 필터링
        return mandatoryPolicies.stream()
                .filter(p -> !agreedPolicyIds.contains(p.getId()))
                .map(PolicyResponseDto::from)
                .collect(Collectors.toList());
    }
}

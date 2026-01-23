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
        // 1. 모든 필수 정책 조회
        List<Policy> mandatoryPolicies = policyMapper.findMandatoryActivePolicies();

        // 2. 필수 정책 동의 여부 검증
        Set<Long> agreedIds = new HashSet<>(policyIds != null ? policyIds : List.of());

        List<String> missingPolicies = mandatoryPolicies.stream()
                .filter(policy -> !agreedIds.contains(policy.getId()))
                .map(Policy::getTitle)
                .collect(Collectors.toList());

        if (!missingPolicies.isEmpty()) {
            throw new BusinessException("필수 약관에 동의해야 합니다: " + String.join(", ", missingPolicies));
        }

        // 3. 동의 저장
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
}

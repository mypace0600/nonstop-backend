package com.app.nonstop.mapper;

import com.app.nonstop.domain.policy.dto.UserPolicyAgreementDto;
import com.app.nonstop.domain.policy.entity.Policy;
import com.app.nonstop.domain.policy.entity.UserPolicyAgreement;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PolicyMapper {
    List<Policy> findAllActive();
    List<Policy> findMandatoryActivePolicies();
    List<UserPolicyAgreementDto> findAgreementsByUserId(@Param("userId") Long userId);
    void saveAgreement(UserPolicyAgreement agreement);

    // Batch insert if needed, but single insert in loop is fine for small number of policies
    void saveAgreements(@Param("agreements") List<UserPolicyAgreement> agreements);
}

package com.app.nonstop.domain.policy.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class UserPolicyAgreement {
    private Long id;
    private Long userId;
    private Long policyId;
    private LocalDateTime agreedAt;
}

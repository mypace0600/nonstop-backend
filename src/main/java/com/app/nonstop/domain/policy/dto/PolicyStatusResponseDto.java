package com.app.nonstop.domain.policy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "정책 동의 상태 응답 DTO")
public class PolicyStatusResponseDto {

    @Schema(description = "필수 정책 동의 완료 여부", example = "true")
    private boolean hasAgreedAllMandatory;

    @Schema(description = "미동의 필수 정책 목록")
    private List<PolicyResponseDto> missingPolicies;
}

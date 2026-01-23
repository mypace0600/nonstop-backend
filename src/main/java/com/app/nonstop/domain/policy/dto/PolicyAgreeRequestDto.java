package com.app.nonstop.domain.policy.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@Schema(description = "정책 동의 요청 DTO")
public class PolicyAgreeRequestDto {
    @Schema(description = "동의한 정책 ID 목록", example = "[1, 2]")
    private List<Long> policyIds;
}

package com.app.nonstop.domain.policy.dto;

import com.app.nonstop.domain.policy.entity.Policy;
import com.app.nonstop.domain.policy.entity.PolicyType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "정책 정보 응답 DTO")
public class PolicyResponseDto {

    @Schema(description = "정책 ID", example = "1")
    private Long id;

    @Schema(description = "정책 유형", example = "TERMS_OF_SERVICE")
    private PolicyType type;

    @Schema(description = "정책 제목", example = "서비스 이용약관")
    private String title;

    @Schema(description = "정책 URL", example = "https://example.com/terms")
    private String url;

    @Schema(description = "필수 여부", example = "true")
    private Boolean isMandatory;

    public static PolicyResponseDto from(Policy policy) {
        return PolicyResponseDto.builder()
                .id(policy.getId())
                .type(policy.getType())
                .title(policy.getTitle())
                .url(policy.getUrl())
                .isMandatory(policy.getIsMandatory())
                .build();
    }
}

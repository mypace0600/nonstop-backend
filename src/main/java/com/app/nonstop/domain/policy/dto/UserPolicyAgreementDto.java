package com.app.nonstop.domain.policy.dto;

import com.app.nonstop.domain.policy.entity.PolicyType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 정책 동의 내역 응답 DTO")
public class UserPolicyAgreementDto {

    @Schema(description = "정책 ID", example = "1")
    private Long policyId;

    @Schema(description = "정책 제목", example = "서비스 이용약관")
    private String title;
    
    @Schema(description = "정책 유형", example = "TERMS_OF_SERVICE")
    private PolicyType type;

    @Schema(description = "정책 URL", example = "https://example.com/terms")
    private String url;

    @Schema(description = "동의 일시")
    private LocalDateTime agreedAt;
}

package com.app.nonstop.domain.user.dto;

import com.app.nonstop.domain.user.entity.User;
import com.app.nonstop.domain.user.entity.VerificationMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "인증 상태 조회 응답 DTO")
public class VerificationStatusResponseDto {

    @Schema(description = "대학생 인증 여부", example = "true")
    private Boolean isVerified;

    @Schema(description = "대학생 인증 방식 (EMAIL_DOMAIN, MANUAL_REVIEW, STUDENT_ID_PHOTO)", example = "EMAIL_DOMAIN")
    private VerificationMethod verificationMethod;

    public static VerificationStatusResponseDto of(User user) {
        return VerificationStatusResponseDto.builder()
                .isVerified(user.getIsVerified())
                .verificationMethod(user.getVerificationMethod())
                .build();
    }
}

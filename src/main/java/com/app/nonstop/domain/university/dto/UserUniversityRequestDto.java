package com.app.nonstop.domain.university.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserUniversityRequestDto {

    @NotNull(message = "대학교 ID는 필수입니다")
    private Long universityId;

    private Long majorId;  // 전공은 선택사항
}

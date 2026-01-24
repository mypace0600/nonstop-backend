package com.app.nonstop.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@Schema(description = "생년월일 등록 요청 DTO")
public class BirthDateUpdateRequestDto {

    @NotNull(message = "생년월일은 필수 입력 항목입니다.")
    @Past(message = "생년월일은 과거 날짜여야 합니다.")
    @Schema(description = "생년월일", example = "2000-01-01")
    private LocalDate birthDate;
}

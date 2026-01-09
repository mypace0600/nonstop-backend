package com.app.nonstop.domain.university.dto;

import com.app.nonstop.domain.university.entity.University;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UniversityResponseDto {
    private Long id;
    private String name;
    private String region;
    private String logoImageUrl;

    public static UniversityResponseDto from(University university) {
        return UniversityResponseDto.builder()
                .id(university.getId())
                .name(university.getName())
                .region(university.getRegion())
                .logoImageUrl(university.getLogoImageUrl())
                .build();
    }
}

package com.app.nonstop.domain.university.dto;

import com.app.nonstop.domain.major.entity.Major;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MajorResponseDto {
    private Long id;
    private String name;

    public static MajorResponseDto from(Major major) {
        return MajorResponseDto.builder()
                .id(major.getId())
                .name(major.getName())
                .build();
    }
}

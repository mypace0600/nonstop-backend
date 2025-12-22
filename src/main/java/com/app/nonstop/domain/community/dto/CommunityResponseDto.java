package com.app.nonstop.domain.community.dto;

import com.app.nonstop.domain.community.entity.Community;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommunityResponseDto {
    private Long id;
    private String name;
    private String description;
    private String icon;
    private Boolean isAnonymous;

    public static CommunityResponseDto from(Community community) {
        return CommunityResponseDto.builder()
                .id(community.getId())
                .name(community.getName())
                .description(community.getDescription())
                .icon(community.getIcon())
                .isAnonymous(community.getIsAnonymous())
                .build();
    }
}

package com.app.nonstop.domain.community.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.List;

@Getter
@Builder
public class CommunityListWrapper {
    private List<CommunityResponseDto> communities;
    private boolean universityRequired;
}

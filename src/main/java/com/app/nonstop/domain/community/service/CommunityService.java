package com.app.nonstop.domain.community.service;

import com.app.nonstop.domain.community.dto.CommunityListWrapper;

public interface CommunityService {
    CommunityListWrapper getCommunities(Long universityId, Boolean isUniversityVerified);
}

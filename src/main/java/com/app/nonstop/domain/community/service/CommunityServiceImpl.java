package com.app.nonstop.domain.community.service;

import com.app.nonstop.domain.community.dto.CommunityListWrapper;
import com.app.nonstop.domain.community.dto.CommunityResponseDto;
import com.app.nonstop.domain.community.mapper.CommunityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityServiceImpl implements CommunityService {

    private final CommunityMapper communityMapper;

    @Override
    public CommunityListWrapper getCommunities(Long universityId, Boolean isVerified) {
        // universityId가 없거나 isVerified가 false면 universityRequired를 true로 설정하고 빈 리스트 반환
        if (universityId == null || !Boolean.TRUE.equals(isVerified)) {
            return CommunityListWrapper.builder()
                    .communities(Collections.emptyList())
                    .universityRequired(true)
                    .build();
        }

        List<CommunityResponseDto> communities = communityMapper.findByUniversityId(universityId)
                .stream()
                .map(CommunityResponseDto::from)
                .collect(Collectors.toList());

        return CommunityListWrapper.builder()
                .communities(communities)
                .universityRequired(false)
                .build();
    }
}

package com.app.nonstop.domain.community.service;

import com.app.nonstop.domain.community.dto.CommunityListWrapper;
import com.app.nonstop.domain.community.dto.CommunityResponseDto;
import com.app.nonstop.mapper.CommunityMapper;
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
        // 인증된 사용자라면 본인 대학 ID를 사용, 아니라면 null (공통 커뮤니티만 조회)
        Long queryUniversityId = (Boolean.TRUE.equals(isVerified)) ? universityId : null;

        List<CommunityResponseDto> communities = communityMapper.findByUniversityId(queryUniversityId)
                .stream()
                .map(CommunityResponseDto::from)
                .collect(Collectors.toList());

        // 미인증 상태라면 '학교 인증 필요' 플래그는 true로 유지 (프론트엔드 안내용)
        boolean universityRequired = universityId == null || !Boolean.TRUE.equals(isVerified);

        return CommunityListWrapper.builder()
                .communities(communities)
                .universityRequired(universityRequired)
                .build();
    }
}

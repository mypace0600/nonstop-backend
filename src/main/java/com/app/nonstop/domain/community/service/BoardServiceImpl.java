package com.app.nonstop.domain.community.service;

import com.app.nonstop.domain.community.dto.BoardResponseDto;
import com.app.nonstop.domain.community.entity.Community;
import com.app.nonstop.global.common.exception.AccessDeniedException;
import com.app.nonstop.global.common.exception.BusinessException;
import com.app.nonstop.global.common.exception.ResourceNotFoundException;
import com.app.nonstop.mapper.BoardMapper;
import com.app.nonstop.mapper.CommunityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardServiceImpl implements BoardService {

    private final BoardMapper boardMapper;
    private final CommunityMapper communityMapper;

    @Override
    public List<BoardResponseDto> getBoardsByCommunityId(Long communityId, Long universityId, Boolean isVerified) {
        Community community = communityMapper.findById(communityId)
                .orElseThrow(() -> new ResourceNotFoundException("Community not found"));

        if (!Boolean.TRUE.equals(isVerified)) {
             throw new BusinessException("University verification required");
        }

        if (!community.getUniversityId().equals(universityId)) {
            throw new AccessDeniedException("You do not have access to this community");
        }

        return boardMapper.findByCommunityId(communityId)
                .stream()
                .map(BoardResponseDto::from)
                .collect(Collectors.toList());
    }
}

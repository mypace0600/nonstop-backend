package com.app.nonstop.domain.community.service;

import com.app.nonstop.domain.community.dto.BoardResponseDto;

import java.util.List;

public interface BoardService {
    List<BoardResponseDto> getBoardsByCommunityId(Long communityId, Long universityId, Boolean isVerified);
}

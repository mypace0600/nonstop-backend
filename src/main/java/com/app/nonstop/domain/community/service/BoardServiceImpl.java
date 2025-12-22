package com.app.nonstop.domain.community.service;

import com.app.nonstop.domain.community.dto.BoardResponseDto;
import com.app.nonstop.domain.community.mapper.BoardMapper;
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

    @Override
    public List<BoardResponseDto> getBoardsByCommunityId(Long communityId) {
        return boardMapper.findByCommunityId(communityId)
                .stream()
                .map(BoardResponseDto::from)
                .collect(Collectors.toList());
    }
}

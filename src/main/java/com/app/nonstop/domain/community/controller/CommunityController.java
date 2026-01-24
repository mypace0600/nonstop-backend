package com.app.nonstop.domain.community.controller;

import com.app.nonstop.domain.community.dto.BoardResponseDto;
import com.app.nonstop.domain.community.dto.CommunityListWrapper;
import com.app.nonstop.domain.community.service.BoardService;
import com.app.nonstop.domain.community.service.CommunityService;
import com.app.nonstop.global.common.response.ApiResponse;
import com.app.nonstop.global.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;
    private final BoardService boardService;

    @GetMapping("/communities")
    public ApiResponse<CommunityListWrapper> getCommunities(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
        CommunityListWrapper communities = communityService.getCommunities(customUserDetails.getUniversityId(), customUserDetails.getIsUniversityVerified());
        return ApiResponse.success(communities);
    }

    @GetMapping("/communities/{communityId}/boards")
    public ApiResponse<List<BoardResponseDto>> getBoardsByCommunityId(
            @PathVariable Long communityId,
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        List<BoardResponseDto> boards = boardService.getBoardsByCommunityId(
                communityId,
                customUserDetails.getUniversityId(),
                customUserDetails.getIsUniversityVerified()
        );
        return ApiResponse.success(boards);
    }
}

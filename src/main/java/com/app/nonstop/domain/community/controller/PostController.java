package com.app.nonstop.domain.community.controller;

import com.app.nonstop.domain.community.dto.PostRequestDto;
import com.app.nonstop.domain.community.dto.PostResponseDto;
import com.app.nonstop.domain.community.service.PostService;
import com.app.nonstop.global.common.response.ApiResponse;
import com.app.nonstop.global.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // 게시판 별 게시글 목록
    @GetMapping("/boards/{boardId}/posts")
    public ApiResponse<List<PostResponseDto>> getPostList(
            @PathVariable("boardId") Long boardId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = (userDetails != null) ? userDetails.getUserId() : null;
        return ApiResponse.success(postService.getPostList(boardId, page, size, userId));
    }

    // 게시글 작성
    @PostMapping("/boards/{boardId}/posts")
    public ApiResponse<PostResponseDto> createPost(
            @PathVariable("boardId") Long boardId,
            @RequestBody PostRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.success(postService.createPost(boardId, userDetails.getUserId(), requestDto));
    }

    // 게시글 상세
    @GetMapping("/posts/{postId}")
    public ApiResponse<PostResponseDto> getPostDetail(
            @PathVariable("postId") Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = (userDetails != null) ? userDetails.getUserId() : null;
        return ApiResponse.success(postService.getPostDetail(postId, userId));
    }

    // 게시글 수정
    @PatchMapping("/posts/{postId}")
    public ApiResponse<PostResponseDto> updatePost(
            @PathVariable("postId") Long postId,
            @RequestBody PostRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.success(postService.updatePost(postId, userDetails.getUserId(), requestDto));
    }

    // 게시글 삭제
    @DeleteMapping("/posts/{postId}")
    public ApiResponse<?> deletePost(
            @PathVariable("postId") Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        postService.deletePost(postId, userDetails.getUserId());
        return ApiResponse.success();
    }

    // 게시글 좋아요
    @PostMapping("/posts/{postId}/like")
    public ApiResponse<?> toggleLike(
            @PathVariable("postId") Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        postService.toggleLike(postId, userDetails.getUserId());
        return ApiResponse.success();
    }
}

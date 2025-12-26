package com.app.nonstop.domain.community.controller;

import com.app.nonstop.domain.community.dto.PostDto;
import com.app.nonstop.domain.community.service.PostService;
import com.app.nonstop.global.common.response.ApiResponse;
import com.app.nonstop.global.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 게시글 API 컨트롤러.
 * /api/v1/boards/{boardId}/posts 및 /api/v1/posts 관련 엔드포인트를 처리합니다.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * 게시판 별 게시글 목록 조회.
     * 페이징 처리를 포함합니다.
     */
    @GetMapping("/boards/{boardId}/posts")
    public ApiResponse<List<PostDto.Response>> getPostList(
            @PathVariable("boardId") Long boardId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = (userDetails != null) ? userDetails.getUserId() : null;
        return ApiResponse.success(postService.getPostList(boardId, page, size, userId));
    }

    /**
     * 게시글 작성.
     * 대학 인증이 필요합니다.
     */
    @PostMapping("/boards/{boardId}/posts")
    public ApiResponse<PostDto.Response> createPost(
            @PathVariable("boardId") Long boardId,
            @RequestBody @Valid PostDto.Request requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.success(postService.createPost(boardId, userDetails, requestDto));
    }

    /**
     * 게시글 상세 조회.
     */
    @GetMapping("/posts/{postId}")
    public ApiResponse<PostDto.Response> getPostDetail(
            @PathVariable("postId") Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = (userDetails != null) ? userDetails.getUserId() : null;
        return ApiResponse.success(postService.getPostDetail(postId, userId));
    }

    /**
     * 게시글 수정.
     */
    @PatchMapping("/posts/{postId}")
    public ApiResponse<PostDto.Response> updatePost(
            @PathVariable("postId") Long postId,
            @RequestBody @Valid PostDto.Request requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.success(postService.updatePost(postId, userDetails.getUserId(), requestDto));
    }

    /**
     * 게시글 삭제.
     */
    @DeleteMapping("/posts/{postId}")
    public ApiResponse<?> deletePost(
            @PathVariable("postId") Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        postService.deletePost(postId, userDetails.getUserId());
        return ApiResponse.success();
    }

    /**
     * 게시글 좋아요 토글.
     */
    @PostMapping("/posts/{postId}/like")
    public ApiResponse<?> toggleLike(
            @PathVariable("postId") Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        postService.toggleLike(postId, userDetails.getUserId());
        return ApiResponse.success();
    }
}

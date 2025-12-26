package com.app.nonstop.domain.community.controller;

import com.app.nonstop.domain.community.dto.CommentDto;
import com.app.nonstop.domain.community.service.CommentService;
import com.app.nonstop.global.common.response.ApiResponse;
import com.app.nonstop.global.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 댓글 API 컨트롤러.
 * /api/v1/posts/{postId}/comments 및 /api/v1/comments 관련 엔드포인트를 처리합니다.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 댓글 목록 조회.
     * 계층형(Tree) 구조로 반환합니다.
     */
    @GetMapping("/posts/{postId}/comments")
    public ApiResponse<List<CommentDto.Response>> getComments(
            @PathVariable("postId") Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = (userDetails != null) ? userDetails.getUserId() : null;
        return ApiResponse.success(commentService.getComments(postId, userId));
    }

    /**
     * 댓글 작성.
     */
    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<CommentDto.Response> createComment(
            @PathVariable("postId") Long postId,
            @RequestBody @Valid CommentDto.Request requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.success(commentService.createComment(postId, userDetails.getUserId(), requestDto));
    }

    /**
     * 댓글 삭제.
     */
    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<?> deleteComment(
            @PathVariable("commentId") Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        commentService.deleteComment(commentId, userDetails.getUserId());
        return ApiResponse.success();
    }

    /**
     * 댓글 좋아요 토글.
     */
    @PostMapping("/comments/{commentId}/like")
    public ApiResponse<?> toggleLike(
            @PathVariable("commentId") Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        commentService.toggleLike(commentId, userDetails.getUserId());
        return ApiResponse.success();
    }
}

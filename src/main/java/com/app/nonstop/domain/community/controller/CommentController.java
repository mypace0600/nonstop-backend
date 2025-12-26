package com.app.nonstop.domain.community.controller;

import com.app.nonstop.domain.community.dto.CommentRequestDto;
import com.app.nonstop.domain.community.dto.CommentResponseDto;
import com.app.nonstop.domain.community.service.CommentService;
import com.app.nonstop.global.common.response.ApiResponse;
import com.app.nonstop.global.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // 댓글 목록 (계층형) - PostController에 둘 수도 있지만, 여기 둠
    @GetMapping("/posts/{postId}/comments")
    public ApiResponse<List<CommentResponseDto>> getComments(
            @PathVariable("postId") Long postId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = (userDetails != null) ? userDetails.getUserId() : null;
        return ApiResponse.success(commentService.getComments(postId, userId));
    }

    // 댓글 작성
    @PostMapping("/posts/{postId}/comments")
    public ApiResponse<CommentResponseDto> createComment(
            @PathVariable("postId") Long postId,
            @RequestBody CommentRequestDto requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.success(commentService.createComment(postId, userDetails.getUserId(), requestDto));
    }

    // 댓글 삭제
    @DeleteMapping("/comments/{commentId}")
    public ApiResponse<?> deleteComment(
            @PathVariable("commentId") Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        commentService.deleteComment(commentId, userDetails.getUserId());
        return ApiResponse.success();
    }

    // 댓글 좋아요
    @PostMapping("/comments/{commentId}/like")
    public ApiResponse<?> toggleLike(
            @PathVariable("commentId") Long commentId,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        commentService.toggleLike(commentId, userDetails.getUserId());
        return ApiResponse.success();
    }
}

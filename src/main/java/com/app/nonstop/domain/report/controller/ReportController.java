package com.app.nonstop.domain.report.controller;

import com.app.nonstop.domain.report.dto.ReportDto;
import com.app.nonstop.domain.report.entity.ReportTargetType;
import com.app.nonstop.domain.report.service.ReportService;
import com.app.nonstop.global.common.response.ApiResponse;
import com.app.nonstop.global.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 신고 API 컨트롤러.
 * 게시글 및 댓글 신고 엔드포인트를 제공합니다.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * 게시글 신고.
     */
    @PostMapping("/posts/{postId}/report")
    public ApiResponse<?> reportPost(
            @PathVariable("postId") Long postId,
            @RequestBody @Valid ReportDto.Request requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        reportService.createReport(userDetails.getUserId(), ReportTargetType.POST, postId, requestDto);
        return ApiResponse.success();
    }

    /**
     * 댓글 신고.
     */
    @PostMapping("/comments/{commentId}/report")
    public ApiResponse<?> reportComment(
            @PathVariable("commentId") Long commentId,
            @RequestBody @Valid ReportDto.Request requestDto,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        reportService.createReport(userDetails.getUserId(), ReportTargetType.COMMENT, commentId, requestDto);
        return ApiResponse.success();
    }
}

package com.app.nonstop.domain.report.controller;

import com.app.nonstop.domain.report.dto.ReportDto;
import com.app.nonstop.domain.report.entity.ReportTargetType;
import com.app.nonstop.domain.report.service.ReportService;
import com.app.nonstop.global.common.response.ApiResponse;
import com.app.nonstop.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 신고 API 컨트롤러.
 * 게시글, 댓글, 사용자, 채팅 메시지 신고 엔드포인트를 제공합니다.
 */
@Tag(name = "Report", description = "신고 API - 게시글, 댓글, 사용자, 채팅 메시지 신고")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ReportController {

    private final ReportService reportService;

    /**
     * 게시글 신고.
     */
    @Operation(summary = "게시글 신고", description = "게시글을 신고합니다. 중복 신고는 불가합니다.")
    @PostMapping("/posts/{postId}/report")
    public ApiResponse<?> reportPost(
            @Parameter(description = "게시글 ID", required = true) @PathVariable("postId") Long postId,
            @RequestBody @Valid ReportDto.Request requestDto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        reportService.createReport(userDetails.getUserId(), ReportTargetType.POST, postId, requestDto);
        return ApiResponse.success();
    }

    /**
     * 댓글 신고.
     */
    @Operation(summary = "댓글 신고", description = "댓글을 신고합니다. 중복 신고는 불가합니다.")
    @PostMapping("/comments/{commentId}/report")
    public ApiResponse<?> reportComment(
            @Parameter(description = "댓글 ID", required = true) @PathVariable("commentId") Long commentId,
            @RequestBody @Valid ReportDto.Request requestDto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        reportService.createReport(userDetails.getUserId(), ReportTargetType.COMMENT, commentId, requestDto);
        return ApiResponse.success();
    }

    /**
     * 사용자 신고.
     */
    @Operation(summary = "사용자 신고", description = "사용자를 신고합니다. 중복 신고는 불가합니다.")
    @PostMapping("/users/{userId}/report")
    public ApiResponse<?> reportUser(
            @Parameter(description = "신고할 사용자 ID", required = true) @PathVariable("userId") Long userId,
            @RequestBody @Valid ReportDto.Request requestDto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        reportService.createReport(userDetails.getUserId(), ReportTargetType.USER, userId, requestDto);
        return ApiResponse.success();
    }

    /**
     * 채팅 메시지 신고.
     */
    @Operation(summary = "채팅 메시지 신고", description = "채팅 메시지를 신고합니다. 중복 신고는 불가합니다.")
    @PostMapping("/chat/messages/{messageId}/report")
    public ApiResponse<?> reportChatMessage(
            @Parameter(description = "채팅 메시지 ID", required = true) @PathVariable("messageId") Long messageId,
            @RequestBody @Valid ReportDto.Request requestDto,
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        reportService.createReport(userDetails.getUserId(), ReportTargetType.CHAT_MESSAGE, messageId, requestDto);
        return ApiResponse.success();
    }
}

package com.app.nonstop.domain.friend.controller;

import com.app.nonstop.domain.friend.dto.FriendDto;
import com.app.nonstop.domain.friend.service.FriendService;
import com.app.nonstop.global.common.response.ApiResponse;
import com.app.nonstop.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Friend & Block", description = "친구 관계 및 사용자 차단 API")
@RestController
@RequestMapping("/api/v1/friends")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class FriendController {

    private final FriendService friendService;

    @Operation(summary = "친구 목록 조회", description = "현재 사용자의 친구 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<FriendDto.FriendResponseDto>>> getMyFriendList(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<FriendDto.FriendResponseDto> friendList = friendService.getFriendList(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(friendList));
    }

    @Operation(summary = "받은 친구 요청 목록 조회", description = "현재 사용자가 받은 PENDING 상태의 친구 요청 목록을 조회합니다.")
    @GetMapping("/requests")
    public ResponseEntity<ApiResponse<List<FriendDto.FriendRequestResponseDto>>> getMyReceivedFriendRequests(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<FriendDto.FriendRequestResponseDto> requestList = friendService.getReceivedFriendRequests(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(requestList));
    }

    @Operation(summary = "친구 요청 보내기", description = "다른 사용자에게 친구 요청을 보냅니다.")
    @PostMapping("/request")
    public ResponseEntity<ApiResponse<?>> sendFriendRequest(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid FriendDto.FriendRequestSendDto requestDto
    ) {
        friendService.sendFriendRequest(userDetails.getUserId(), requestDto);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "친구 요청 수락", description = "받은 친구 요청을 수락합니다.")
    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<ApiResponse<?>> acceptFriendRequest(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long requestId
    ) {
        friendService.acceptFriendRequest(userDetails.getUserId(), requestId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "친구 요청 거절", description = "받은 친구 요청을 거절합니다.")
    @PostMapping("/requests/{requestId}/reject")
    public ResponseEntity<ApiResponse<?>> rejectFriendRequest(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long requestId
    ) {
        friendService.rejectFriendRequest(userDetails.getUserId(), requestId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "보낸 친구 요청 취소", description = "내가 보낸 친구 요청을 취소합니다.")
    @DeleteMapping("/requests/{requestId}")
    public ResponseEntity<ApiResponse<?>> cancelFriendRequest(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long requestId
    ) {
        friendService.cancelFriendRequest(userDetails.getUserId(), requestId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "사용자 차단하기", description = "특정 사용자를 차단합니다. 차단된 사용자와는 친구 요청 및 채팅이 불가능해집니다.")
    @PostMapping("/block")
    public ResponseEntity<ApiResponse<?>> blockUser(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid FriendDto.UserBlockRequestDto requestDto
    ) {
        friendService.blockUser(userDetails.getUserId(), requestDto);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "사용자 차단 해제", description = "차단한 사용자의 차단을 해제합니다.")
    @DeleteMapping("/block/{blockedId}")
    public ResponseEntity<ApiResponse<?>> unblockUser(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "차단 해제할 사용자 ID", required = true) @PathVariable Long blockedId
    ) {
        friendService.unblockUser(userDetails.getUserId(), blockedId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @Operation(summary = "차단 목록 조회", description = "현재 사용자가 차단한 사용자 목록을 조회합니다.")
    @GetMapping("/blocked")
    public ResponseEntity<ApiResponse<List<FriendDto.BlockedUserResponseDto>>> getBlockedUsers(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<FriendDto.BlockedUserResponseDto> blockedUsers = friendService.getBlockedUsers(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(blockedUsers));
    }

    @Operation(summary = "친구 삭제", description = "친구 관계를 해제합니다.")
    @DeleteMapping("/{friendId}")
    public ResponseEntity<ApiResponse<?>> removeFriend(
            @Parameter(hidden = true) @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long friendId
    ) {
        friendService.removeFriend(userDetails.getUserId(), friendId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}

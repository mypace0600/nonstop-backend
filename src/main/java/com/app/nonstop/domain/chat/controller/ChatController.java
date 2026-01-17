package com.app.nonstop.domain.chat.controller;

import com.app.nonstop.domain.chat.dto.*;
import com.app.nonstop.domain.chat.service.ChatRoomService;
import com.app.nonstop.domain.chat.service.ChatService;
import com.app.nonstop.global.common.response.ApiResponse;
import com.app.nonstop.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Chat Room", description = "채팅방 관리 API")
@RestController
@RequestMapping("/api/v1/chat/rooms")
@RequiredArgsConstructor
public class ChatController {

    private final ChatRoomService chatRoomService;
    private final ChatService chatService;

    @Operation(summary = "내 채팅방 목록 조회")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ChatRoomResponseDto>>> getMyChatRooms(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ChatRoomResponseDto> chatRooms = chatRoomService.getMyChatRooms(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(chatRooms));
    }

    @Operation(summary = "채팅방 생성 (1:1 또는 그룹)")
    @PostMapping
    public ResponseEntity<ApiResponse<ChatRoomResponseDto>> createChatRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateChatRoomRequestDto requestDto) {
        
        ChatRoomResponseDto chatRoom;

        if (requestDto.isGroupChat()) {
            if (requestDto.getRoomName() == null || requestDto.getRoomName().isBlank()) {
                throw new IllegalArgumentException("그룹 채팅방 이름은 필수입니다.");
            }
            chatRoom = chatRoomService.createGroupChatRoom(userDetails.getUserId(), requestDto.getRoomName(), requestDto.getUserIds());
        } else if (requestDto.getTargetUserId() != null) {
            chatRoom = chatRoomService.getOrCreateOneToOneChatRoom(userDetails.getUserId(), requestDto.getTargetUserId());
        } else {
            throw new IllegalArgumentException("1:1 채팅 생성을 위한 targetUserId 또는 그룹 채팅 생성을 위한 userIds가 필요합니다.");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(chatRoom));
    }

    @Operation(summary = "과거 메시지 조회")
    @GetMapping("/{roomId}/messages")
    public ResponseEntity<ApiResponse<List<MessageResponseDto>>> getMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        List<MessageResponseDto> messages = chatService.getMessages(roomId, userDetails.getUserId(), limit, offset);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @Operation(summary = "메시지 읽음 처리")
    @PatchMapping("/{roomId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId,
            @RequestParam Long messageId) {
        chatRoomService.markAsRead(roomId, userDetails.getUserId(), messageId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "채팅방 나가기")
    @DeleteMapping("/{roomId}")
    public ResponseEntity<ApiResponse<Void>> leaveChatRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId) {
        chatRoomService.leaveChatRoom(roomId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "메시지 나에게만 삭제")
    @DeleteMapping("/{roomId}/messages/{messageId}")
    public ResponseEntity<ApiResponse<Void>> deleteMessageForMe(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId,
            @PathVariable Long messageId) {
        chatService.deleteMessageForMe(roomId, userDetails.getUserId(), messageId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "채팅방 정보 수정 (그룹)")
    @PatchMapping("/{roomId}")
    public ResponseEntity<ApiResponse<Void>> updateChatRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId,
            @Valid @RequestBody ChatRoomUpdateRequestDto requestDto) {
        // 서비스 내부에서 권한 및 그룹 채팅방 여부 확인
        chatRoomService.updateGroupChatRoom(roomId, userDetails.getUserId(), requestDto.getName());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "채팅방 참여자 목록 조회 (그룹)")
    @GetMapping("/{roomId}/members")
    public ResponseEntity<ApiResponse<List<ChatRoomMemberResponseDto>>> getChatRoomMembers(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId) {
        List<ChatRoomMemberResponseDto> members = chatRoomService.getGroupChatRoomMembers(roomId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(members));
    }

    @Operation(summary = "채팅방 사용자 초대 (그룹)")
    @PostMapping("/{roomId}/invite")
    public ResponseEntity<ApiResponse<Void>> inviteToChatRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId,
            @Valid @RequestBody InviteRequestDto requestDto) {
        chatRoomService.inviteToGroupChatRoom(roomId, userDetails.getUserId(), requestDto.getUserIds());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "채팅방 사용자 강퇴 (그룹/방장 권한)")
    @DeleteMapping("/{roomId}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> kickFromChatRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId,
            @PathVariable Long userId) {
        chatRoomService.kickFromGroupChatRoom(roomId, userDetails.getUserId(), userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
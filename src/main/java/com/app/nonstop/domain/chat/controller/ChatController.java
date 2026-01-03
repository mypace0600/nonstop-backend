package com.app.nonstop.domain.chat.controller;

import com.app.nonstop.domain.chat.dto.*;
import com.app.nonstop.domain.chat.service.ChatRoomService;
import com.app.nonstop.domain.chat.service.ChatService;
import com.app.nonstop.global.common.response.ApiResponse;
import com.app.nonstop.global.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat/rooms")
@RequiredArgsConstructor
public class ChatController {

    private final ChatRoomService chatRoomService;
    private final ChatService chatService;

    /**
     * 내 채팅방 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ChatRoomResponseDto>>> getMyChatRooms(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<ChatRoomResponseDto> chatRooms = chatRoomService.getMyChatRooms(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(chatRooms));
    }

    /**
     * 1:1 채팅방 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ChatRoomResponseDto>> createOneToOneChatRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody OneToOneChatRoomRequestDto requestDto) {
        ChatRoomResponseDto chatRoom = chatRoomService.getOrCreateOneToOneChatRoom(userDetails.getUserId(), requestDto.getTargetUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(chatRoom));
    }

    /**
     * 그룹 채팅방 생성
     */
    @PostMapping("/group-rooms")
    public ResponseEntity<ApiResponse<ChatRoomResponseDto>> createGroupChatRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody GroupChatRoomRequestDto requestDto) {
        ChatRoomResponseDto chatRoom = chatRoomService.createGroupChatRoom(userDetails.getUserId(), requestDto.getRoomName(), requestDto.getUserIds());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(chatRoom));
    }

    /**
     * 과거 메시지 조회 (페이지네이션)
     */
    @GetMapping("/{roomId}/messages")
    public ResponseEntity<ApiResponse<List<MessageResponseDto>>> getMessages(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        List<MessageResponseDto> messages = chatService.getMessages(roomId, userDetails.getUserId(), limit, offset);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    /**
     * 읽음 처리 (채팅방의 마지막 읽은 메시지 업데이트)
     */
    @PatchMapping("/{roomId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId,
            @RequestParam Long messageId) {
        chatRoomService.markAsRead(roomId, userDetails.getUserId(), messageId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 채팅방 나가기
     */
    @DeleteMapping("/{roomId}")
    public ResponseEntity<ApiResponse<Void>> leaveChatRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId) {
        chatRoomService.leaveChatRoom(roomId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 나에게만 메시지 삭제
     */
    @DeleteMapping("/{roomId}/messages/{messageId}")
    public ResponseEntity<ApiResponse<Void>> deleteMessageForMe(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId,
            @PathVariable Long messageId) {
        chatService.deleteMessageForMe(roomId, userDetails.getUserId(), messageId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 그룹 채팅방 정보 수정
     */
    @PatchMapping("/group-rooms/{roomId}")
    public ResponseEntity<ApiResponse<Void>> updateGroupChatRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId,
            @Valid @RequestBody ChatRoomUpdateRequestDto requestDto) {
        chatRoomService.updateGroupChatRoom(roomId, userDetails.getUserId(), requestDto.getName());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 그룹 채팅방 참여자 목록 조회
     */
    @GetMapping("/group-rooms/{roomId}/members")
    public ResponseEntity<ApiResponse<List<ChatRoomMemberResponseDto>>> getGroupChatRoomMembers(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId) {
        List<ChatRoomMemberResponseDto> members = chatRoomService.getGroupChatRoomMembers(roomId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(members));
    }

    /**
     * 그룹 채팅방에 사용자 초대
     */
    @PostMapping("/group-rooms/{roomId}/invite")
    public ResponseEntity<ApiResponse<Void>> inviteToGroupChatRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId,
            @Valid @RequestBody InviteRequestDto requestDto) {
        chatRoomService.inviteToGroupChatRoom(roomId, userDetails.getUserId(), requestDto.getUserIds());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 그룹 채팅방에서 사용자 강퇴
     */
    @DeleteMapping("/group-rooms/{roomId}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> kickFromGroupChatRoom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long roomId,
            @PathVariable Long userId) {
        chatRoomService.kickFromGroupChatRoom(roomId, userDetails.getUserId(), userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}

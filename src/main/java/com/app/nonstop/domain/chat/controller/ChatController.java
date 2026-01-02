package com.app.nonstop.domain.chat.controller;

import com.app.nonstop.domain.chat.dto.ChatRoomResponseDto;
import com.app.nonstop.domain.chat.dto.GroupChatRoomRequestDto;
import com.app.nonstop.domain.chat.dto.MessageResponseDto;
import com.app.nonstop.domain.chat.dto.OneToOneChatRoomRequestDto;
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

    // TODO: 채팅방 나가기 (DELETE /api/v1/chat/rooms/{roomId})
    // TODO: 나에게만 메시지 삭제 (DELETE /api/v1/chat/rooms/{roomId}/messages/{msgId})
    // TODO: 그룹 채팅방 정보 수정 (PATCH /api/v1/chat/group-rooms/{roomId})
    // TODO: 그룹 채팅방 참여자 목록 조회 (GET /api/v1/chat/group-rooms/{roomId}/members)
    // TODO: 그룹 채팅방에 사용자 초대 (POST /api/v1/chat/group-rooms/{roomId}/invite)
    // TODO: 그룹 채팅방에서 사용자 강퇴 (DELETE /api/v1/chat/group-rooms/{roomId}/members/{userId})
}

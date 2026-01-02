package com.app.nonstop.domain.chat.controller;

import com.app.nonstop.domain.chat.dto.ChatRoomResponseDto;
import com.app.nonstop.domain.chat.dto.GroupChatRoomRequestDto;
import com.app.nonstop.domain.chat.dto.MessageResponseDto;
import com.app.nonstop.domain.chat.dto.OneToOneChatRoomRequestDto;
import com.app.nonstop.domain.chat.service.ChatRoomService;
import com.app.nonstop.domain.chat.service.ChatService;
import com.app.nonstop.global.common.response.ApiResponse;
import com.app.nonstop.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat/rooms")
@RequiredArgsConstructor
public class ChatController {

    private final ChatRoomService chatRoomService;
    private final ChatService chatService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 내 채팅방 목록 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ChatRoomResponseDto>>> getMyChatRooms(HttpServletRequest request) {
        Long userId = jwtTokenProvider.getUserIdFromRequest(request);
        List<ChatRoomResponseDto> chatRooms = chatRoomService.getMyChatRooms(userId);
        return ApiResponse.success(HttpStatus.OK, chatRooms);
    }

    /**
     * 1:1 채팅방 생성
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ChatRoomResponseDto>> createOneToOneChatRoom(
            HttpServletRequest request,
            @Valid @RequestBody OneToOneChatRoomRequestDto requestDto) {
        Long currentUserId = jwtTokenProvider.getUserIdFromRequest(request);
        ChatRoomResponseDto chatRoom = chatRoomService.getOrCreateOneToOneChatRoom(currentUserId, requestDto.getTargetUserId());
        return ApiResponse.success(HttpStatus.CREATED, chatRoom);
    }

    /**
     * 그룹 채팅방 생성
     */
    @PostMapping("/group-rooms")
    public ResponseEntity<ApiResponse<ChatRoomResponseDto>> createGroupChatRoom(
            HttpServletRequest request,
            @Valid @RequestBody GroupChatRoomRequestDto requestDto) {
        Long currentUserId = jwtTokenProvider.getUserIdFromRequest(request);
        ChatRoomResponseDto chatRoom = chatRoomService.createGroupChatRoom(currentUserId, requestDto.getRoomName(), requestDto.getUserIds());
        return ApiResponse.success(HttpStatus.CREATED, chatRoom);
    }

    /**
     * 과거 메시지 조회 (페이지네이션)
     */
    @GetMapping("/{roomId}/messages")
    public ResponseEntity<ApiResponse<List<MessageResponseDto>>> getMessages(
            HttpServletRequest request,
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        Long userId = jwtTokenProvider.getUserIdFromRequest(request);
        List<MessageResponseDto> messages = chatService.getMessages(roomId, userId, limit, offset);
        return ApiResponse.success(HttpStatus.OK, messages);
    }

    /**
     * 읽음 처리 (채팅방의 마지막 읽은 메시지 업데이트)
     */
    @PatchMapping("/{roomId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            HttpServletRequest request,
            @PathVariable Long roomId,
            @RequestParam Long messageId) {
        Long userId = jwtTokenProvider.getUserIdFromRequest(request);
        chatRoomService.markAsRead(roomId, userId, messageId);
        return ApiResponse.success(HttpStatus.OK, null);
    }

    // TODO: 채팅방 나가기 (DELETE /api/v1/chat/rooms/{roomId})
    // TODO: 나에게만 메시지 삭제 (DELETE /api/v1/chat/rooms/{roomId}/messages/{msgId})
    // TODO: 그룹 채팅방 정보 수정 (PATCH /api/v1/chat/group-rooms/{roomId})
    // TODO: 그룹 채팅방 참여자 목록 조회 (GET /api/v1/chat/group-rooms/{roomId}/members)
    // TODO: 그룹 채팅방에 사용자 초대 (POST /api/v1/chat/group-rooms/{roomId}/invite)
    // TODO: 그룹 채팅방에서 사용자 강퇴 (DELETE /api/v1/chat/group-rooms/{roomId}/members/{userId})
}

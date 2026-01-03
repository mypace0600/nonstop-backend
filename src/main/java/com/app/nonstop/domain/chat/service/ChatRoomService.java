package com.app.nonstop.domain.chat.service;

import com.app.nonstop.domain.chat.dto.ChatRoomMemberResponseDto;
import com.app.nonstop.domain.chat.dto.ChatRoomResponseDto;

import java.util.List;
import java.util.Set;

public interface ChatRoomService {
    List<ChatRoomResponseDto> getMyChatRooms(Long userId);
    ChatRoomResponseDto getOrCreateOneToOneChatRoom(Long currentUserId, Long targetUserId);
    ChatRoomResponseDto createGroupChatRoom(Long currentUserId, String roomName, Set<Long> userIds);
    void markAsRead(Long roomId, Long userId, Long messageId);

    // 채팅방 나가기
    void leaveChatRoom(Long roomId, Long userId);

    // 그룹 채팅방 관리
    List<ChatRoomMemberResponseDto> getGroupChatRoomMembers(Long roomId, Long userId);
    void inviteToGroupChatRoom(Long roomId, Long inviterId, Set<Long> userIds);
    void kickFromGroupChatRoom(Long roomId, Long kickerId, Long targetUserId);
    void updateGroupChatRoom(Long roomId, Long userId, String newName);
}

package com.app.nonstop.domain.chat.service;

import com.app.nonstop.domain.chat.dto.ChatRoomResponseDto;

import java.util.List;
import java.util.Set;

public interface ChatRoomService {
    List<ChatRoomResponseDto> getMyChatRooms(Long userId);
    ChatRoomResponseDto getOrCreateOneToOneChatRoom(Long currentUserId, Long targetUserId);
    ChatRoomResponseDto createGroupChatRoom(Long currentUserId, String roomName, Set<Long> userIds);
}

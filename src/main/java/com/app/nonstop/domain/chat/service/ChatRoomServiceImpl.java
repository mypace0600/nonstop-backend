package com.app.nonstop.domain.chat.service;

import com.app.nonstop.domain.chat.dto.ChatRoomResponseDto;
import com.app.nonstop.domain.chat.entity.ChatRoom;
import com.app.nonstop.domain.chat.entity.ChatRoomMember;
import com.app.nonstop.domain.chat.entity.ChatRoomType;
import com.app.nonstop.domain.user.entity.User; // Assuming User entity exists
import com.app.nonstop.mapper.ChatRoomMapper; // New mapper
import com.app.nonstop.mapper.UserMapper; // Existing user mapper
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {

    private final ChatRoomMapper chatRoomMapper;
    private final UserMapper userMapper; // To get user details

    @Override
    public List<ChatRoomResponseDto> getMyChatRooms(Long userId) {
        return chatRoomMapper.findMyChatRooms(userId);
    }

    @Override
    @Transactional
    public ChatRoomResponseDto getOrCreateOneToOneChatRoom(Long currentUserId, Long targetUserId) {
        // 1. Check if an existing 1:1 chat room exists
        ChatRoom existingRoom = chatRoomMapper.findOneToOneChatRoom(currentUserId, targetUserId);

        if (existingRoom != null) {
            return ChatRoomResponseDto.builder()
                    .roomId(existingRoom.getId())
                    .type(existingRoom.getType())
                    .name(getUserNickname(targetUserId)) // Placeholder
                    // TODO: Populate last message, unread count etc.
                    .build();
        }

        // 2. Create a new 1:1 chat room
        ChatRoom newRoom = ChatRoom.builder()
                .type(ChatRoomType.ONE_TO_ONE)
                .creatorId(currentUserId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        chatRoomMapper.insertChatRoom(newRoom); // room ID will be set by MyBatis

        // 3. Add members to the new room
        chatRoomMapper.insertChatRoomMember(ChatRoomMember.builder()
                .roomId(newRoom.getId())
                .userId(currentUserId)
                .joinedAt(LocalDateTime.now())
                .build());
        chatRoomMapper.insertChatRoomMember(ChatRoomMember.builder()
                .roomId(newRoom.getId())
                .userId(targetUserId)
                .joinedAt(LocalDateTime.now())
                .build());

        // 4. Update one_to_one_chat_rooms table
        chatRoomMapper.insertOneToOneChatRoom(newRoom.getId(), currentUserId, targetUserId);

        return ChatRoomResponseDto.builder()
                .roomId(newRoom.getId())
                .type(newRoom.getType())
                .name(getUserNickname(targetUserId)) // Placeholder
                .build();
    }

    @Override
    @Transactional
    public ChatRoomResponseDto createGroupChatRoom(Long currentUserId, String roomName, Set<Long> userIds) {
        // Ensure current user is part of the members
        Set<Long> allMemberIds = new HashSet<>(userIds);
        allMemberIds.add(currentUserId);

        // 1. Create a new group chat room
        ChatRoom newRoom = ChatRoom.builder()
                .type(ChatRoomType.GROUP)
                .name(roomName)
                .creatorId(currentUserId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        chatRoomMapper.insertChatRoom(newRoom); // room ID will be set by MyBatis

        // 2. Add all members to the new room
        for (Long memberId : allMemberIds) {
            chatRoomMapper.insertChatRoomMember(ChatRoomMember.builder()
                    .roomId(newRoom.getId())
                    .userId(memberId)
                    .joinedAt(LocalDateTime.now())
                    .build());
        }

        return ChatRoomResponseDto.builder()
                .roomId(newRoom.getId())
                .type(newRoom.getType())
                .name(newRoom.getName())
                .build();
    }

    @Override
    @Transactional
    public void markAsRead(Long roomId, Long userId, Long messageId) {
        chatRoomMapper.updateLastReadMessageId(roomId, userId, messageId);
    }

    private String getUserNickname(Long userId) {
        return userMapper.findById(userId)
                .map(User::getNickname)
                .orElse("Unknown User");
    }
}

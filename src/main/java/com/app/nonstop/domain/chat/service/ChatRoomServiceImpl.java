package com.app.nonstop.domain.chat.service;

import com.app.nonstop.domain.chat.dto.ChatMessageDto;
import com.app.nonstop.domain.chat.dto.ChatReadEventDto;
import com.app.nonstop.domain.chat.dto.ChatRoomMemberResponseDto;
import com.app.nonstop.domain.chat.dto.ChatRoomResponseDto;
import com.app.nonstop.domain.chat.entity.ChatRoom;
import com.app.nonstop.domain.chat.entity.ChatRoomMember;
import com.app.nonstop.domain.chat.entity.ChatRoomType;
import com.app.nonstop.domain.chat.entity.MessageType;
import com.app.nonstop.domain.user.entity.User;
import com.app.nonstop.global.common.exception.AccessDeniedException;
import com.app.nonstop.global.common.exception.ResourceNotFoundException;
import com.app.nonstop.mapper.ChatRoomMapper;
import com.app.nonstop.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {

    private final ChatRoomMapper chatRoomMapper;
    private final UserMapper userMapper;
    private final ChatKafkaProducer chatKafkaProducer;
    private final ChatReadEventProducer chatReadEventProducer;

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
    public void markAsRead(Long roomId, Long userId, Long messageId) {
        // 멤버 여부 검증
        if (!chatRoomMapper.isMemberOfRoom(roomId, userId)) {
            throw new AccessDeniedException("You are not a member of this chat room");
        }

        // Kafka로 읽음 이벤트 발행 (비동기 처리)
        ChatReadEventDto event = ChatReadEventDto.builder()
                .roomId(roomId)
                .userId(userId)
                .messageId(messageId)
                .timestamp(LocalDateTime.now())
                .build();

        chatReadEventProducer.sendReadEvent(event);
    }

    @Override
    @Transactional
    public void leaveChatRoom(Long roomId, Long userId) {
        // 채팅방 존재 여부 확인
        ChatRoom chatRoom = chatRoomMapper.findById(roomId);
        if (chatRoom == null) {
            throw new ResourceNotFoundException("Chat room not found: " + roomId);
        }

        // 멤버 여부 확인
        if (!chatRoomMapper.isMemberOfRoom(roomId, userId)) {
            throw new AccessDeniedException("You are not a member of this chat room");
        }

        // 채팅방 나가기
        chatRoomMapper.leaveChatRoom(roomId, userId);

        // 그룹 채팅방인 경우 시스템 메시지 발송
        if (chatRoom.getType() == ChatRoomType.GROUP) {
            String nickname = getUserNickname(userId);
            sendSystemMessage(roomId, userId, MessageType.SYSTEM_LEAVE,
                    nickname + "님이 채팅방을 나갔습니다.");
        }
    }

    @Override
    public List<ChatRoomMemberResponseDto> getGroupChatRoomMembers(Long roomId, Long userId) {
        // 채팅방 존재 여부 확인
        ChatRoom chatRoom = chatRoomMapper.findById(roomId);
        if (chatRoom == null) {
            throw new ResourceNotFoundException("Chat room not found: " + roomId);
        }

        // 그룹 채팅방인지 확인
        if (chatRoom.getType() != ChatRoomType.GROUP) {
            throw new IllegalArgumentException("This is not a group chat room");
        }

        // 멤버 여부 확인
        if (!chatRoomMapper.isMemberOfRoom(roomId, userId)) {
            throw new AccessDeniedException("You are not a member of this chat room");
        }

        return chatRoomMapper.findMembersByRoomId(roomId);
    }

    @Override
    @Transactional
    public void inviteToGroupChatRoom(Long roomId, Long inviterId, Set<Long> userIds) {
        // 채팅방 존재 여부 확인
        ChatRoom chatRoom = chatRoomMapper.findById(roomId);
        if (chatRoom == null) {
            throw new ResourceNotFoundException("Chat room not found: " + roomId);
        }

        // 그룹 채팅방인지 확인
        if (chatRoom.getType() != ChatRoomType.GROUP) {
            throw new IllegalArgumentException("Cannot invite to a 1:1 chat room");
        }

        // 초대자가 멤버인지 확인
        if (!chatRoomMapper.isMemberOfRoom(roomId, inviterId)) {
            throw new AccessDeniedException("You are not a member of this chat room");
        }

        String inviterNickname = getUserNickname(inviterId);

        // 새로운 멤버 추가
        for (Long userId : userIds) {
            // 이미 멤버인지 확인
            if (!chatRoomMapper.isMemberOfRoom(roomId, userId)) {
                chatRoomMapper.insertChatRoomMember(ChatRoomMember.builder()
                        .roomId(roomId)
                        .userId(userId)
                        .joinedAt(LocalDateTime.now())
                        .build());

                String invitedNickname = getUserNickname(userId);
                sendSystemMessage(roomId, inviterId, MessageType.SYSTEM_INVITE,
                        inviterNickname + "님이 " + invitedNickname + "님을 초대했습니다.");
            }
        }
    }

    @Override
    @Transactional
    public void kickFromGroupChatRoom(Long roomId, Long kickerId, Long targetUserId) {
        // 채팅방 존재 여부 확인
        ChatRoom chatRoom = chatRoomMapper.findById(roomId);
        if (chatRoom == null) {
            throw new ResourceNotFoundException("Chat room not found: " + roomId);
        }

        // 그룹 채팅방인지 확인
        if (chatRoom.getType() != ChatRoomType.GROUP) {
            throw new IllegalArgumentException("Cannot kick from a 1:1 chat room");
        }

        // 강퇴자가 방장인지 확인
        if (!chatRoom.getCreatorId().equals(kickerId)) {
            throw new AccessDeniedException("Only the room creator can kick members");
        }

        // 자기 자신을 강퇴할 수 없음
        if (kickerId.equals(targetUserId)) {
            throw new IllegalArgumentException("Cannot kick yourself");
        }

        // 대상이 멤버인지 확인
        if (!chatRoomMapper.isMemberOfRoom(roomId, targetUserId)) {
            throw new ResourceNotFoundException("Target user is not a member of this chat room");
        }

        // 멤버 강퇴
        chatRoomMapper.removeMember(roomId, targetUserId);

        // 시스템 메시지 발송
        String targetNickname = getUserNickname(targetUserId);
        sendSystemMessage(roomId, kickerId, MessageType.SYSTEM_KICK,
                targetNickname + "님이 채팅방에서 내보내졌습니다.");
    }

    @Override
    @Transactional
    public void updateGroupChatRoom(Long roomId, Long userId, String newName) {
        // 채팅방 존재 여부 확인
        ChatRoom chatRoom = chatRoomMapper.findById(roomId);
        if (chatRoom == null) {
            throw new ResourceNotFoundException("Chat room not found: " + roomId);
        }

        // 그룹 채팅방인지 확인
        if (chatRoom.getType() != ChatRoomType.GROUP) {
            throw new IllegalArgumentException("Cannot update a 1:1 chat room");
        }

        // 멤버 여부 확인
        if (!chatRoomMapper.isMemberOfRoom(roomId, userId)) {
            throw new AccessDeniedException("You are not a member of this chat room");
        }

        // 채팅방 이름 업데이트
        chatRoom.setName(newName);
        chatRoomMapper.updateChatRoom(chatRoom);
    }

    private String getUserNickname(Long userId) {
        return userMapper.findById(userId)
                .map(User::getNickname)
                .orElse("Unknown User");
    }

    private void sendSystemMessage(Long roomId, Long senderId, MessageType type, String content) {
        ChatMessageDto systemMessage = new ChatMessageDto();
        systemMessage.setRoomId(roomId);
        systemMessage.setSenderId(senderId);
        systemMessage.setType(type);
        systemMessage.setContent(content);
        chatKafkaProducer.sendMessage("chat-messages", systemMessage);
    }
}

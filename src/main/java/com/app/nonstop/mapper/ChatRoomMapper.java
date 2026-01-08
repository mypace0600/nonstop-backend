package com.app.nonstop.mapper;

import com.app.nonstop.domain.chat.dto.ChatRoomMemberResponseDto;
import com.app.nonstop.domain.chat.dto.ChatRoomResponseDto;
import com.app.nonstop.domain.chat.entity.ChatRoom;
import com.app.nonstop.domain.chat.entity.ChatRoomMember;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatRoomMapper {
    ChatRoom findOneToOneChatRoom(@Param("userAId") Long userAId, @Param("userBId") Long userBId);
    ChatRoom findById(@Param("roomId") Long roomId);
    void insertChatRoom(ChatRoom chatRoom);
    void insertChatRoomMember(ChatRoomMember chatRoomMember);
    void insertOneToOneChatRoom(@Param("roomId") Long roomId, @Param("userAId") Long userAId, @Param("userBId") Long userBId);
    List<ChatRoomResponseDto> findMyChatRooms(@Param("userId") Long userId);
    void updateLastReadMessageId(@Param("roomId") Long roomId, @Param("userId") Long userId, @Param("messageId") Long messageId);

    // 채팅방 나가기
    void leaveChatRoom(@Param("roomId") Long roomId, @Param("userId") Long userId);
    boolean isMemberOfRoom(@Param("roomId") Long roomId, @Param("userId") Long userId);

    // 그룹 채팅방 관리
    List<ChatRoomMemberResponseDto> findMembersByRoomId(@Param("roomId") Long roomId);
    void updateChatRoom(ChatRoom chatRoom);
    void removeMember(@Param("roomId") Long roomId, @Param("userId") Long userId);
    int countActiveMembers(@Param("roomId") Long roomId);
    int countUnreadMessages(@Param("roomId") Long roomId, @Param("userId") Long userId);
}

package com.app.nonstop.mapper;

import com.app.nonstop.domain.chat.dto.ChatMessageDto;
import com.app.nonstop.domain.chat.dto.MessageResponseDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatMapper {
    void insertMessage(ChatMessageDto message);
    List<MessageResponseDto> findMessagesByRoomId(@Param("roomId") Long roomId, @Param("userId") Long userId, @Param("limit") int limit, @Param("offset") int offset);

    // 메시지 삭제 (나에게만)
    void insertMessageDeletion(@Param("messageId") Long messageId, @Param("userId") Long userId);
    boolean isMessageInRoom(@Param("messageId") Long messageId, @Param("roomId") Long roomId);
}

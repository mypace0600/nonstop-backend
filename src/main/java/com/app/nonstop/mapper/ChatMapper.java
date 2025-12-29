package com.app.nonstop.mapper;

import com.app.nonstop.domain.chat.dto.ChatMessageDto;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatMapper {
    void insertMessage(ChatMessageDto message);
}

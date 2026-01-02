package com.app.nonstop.domain.chat.service;


import com.app.nonstop.domain.chat.dto.ChatMessageDto;
import com.app.nonstop.domain.chat.dto.MessageResponseDto;

import java.util.List;

public interface ChatService {
    void saveAndBroadcastMessage(ChatMessageDto message);
    List<MessageResponseDto> getMessages(Long roomId, Long userId, int limit, int offset);
}

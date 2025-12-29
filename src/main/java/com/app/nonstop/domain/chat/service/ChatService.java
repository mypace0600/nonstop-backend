package com.app.nonstop.domain.chat.service;


import com.app.nonstop.domain.chat.dto.ChatMessageDto;

public interface ChatService {
    void saveAndBroadcastMessage(ChatMessageDto message);
}

package com.app.nonstop.domain.chat.dto;

import com.app.nonstop.domain.chat.entity.ChatRoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomResponseDto {
    private Long roomId;
    private ChatRoomType type;
    private String name; // 1:1 채팅방의 경우 상대방 닉네임, 그룹 채팅방의 경우 그룹명
    private String lastMessageContent;
    private LocalDateTime lastMessageSentAt;
    private int unreadCount;
    private String imageUrl; // 1:1 채팅방의 경우 상대방 프로필 이미지, 그룹 채팅방의 경우 그룹 아이콘
}

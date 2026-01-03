package com.app.nonstop.domain.chat.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ChatRoomMemberResponseDto {
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private LocalDateTime joinedAt;
    private boolean isCreator;
}

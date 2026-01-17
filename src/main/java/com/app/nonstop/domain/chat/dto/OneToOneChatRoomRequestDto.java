package com.app.nonstop.domain.chat.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OneToOneChatRoomRequestDto {
    @NotNull(message = "상대방의 유저 ID는 필수입니다.")
    private Long targetUserId;
}

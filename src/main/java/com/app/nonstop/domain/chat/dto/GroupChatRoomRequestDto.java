package com.app.nonstop.domain.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class GroupChatRoomRequestDto {
    @NotBlank(message = "채팅방 이름은 필수입니다.")
    @Size(max = 255, message = "채팅방 이름은 255자를 초과할 수 없습니다.")
    private String roomName;

    @NotEmpty(message = "그룹 채팅 참여자는 최소 1명 이상이어야 합니다.")
    private Set<Long> userIds;
}

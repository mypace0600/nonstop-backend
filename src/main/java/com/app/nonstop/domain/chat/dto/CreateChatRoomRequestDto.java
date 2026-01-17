package com.app.nonstop.domain.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "채팅방 생성 요청 DTO (1:1 및 그룹 공용)")
public class CreateChatRoomRequestDto {

    @Schema(description = "1:1 채팅 상대방 사용자 ID (1:1 채팅 생성 시 필수)", example = "10")
    private Long targetUserId;

    @Schema(description = "채팅방 이름 (그룹 채팅 생성 시 필수)", example = "캡스톤 디자인 팀")
    private String roomName;

    @Schema(description = "초대할 사용자 ID 목록 (그룹 채팅 생성 시 필수)", example = "[10, 12, 15]")
    private Set<Long> userIds;

    public boolean isGroupChat() {
        return userIds != null && !userIds.isEmpty();
    }
}

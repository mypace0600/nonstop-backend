package com.app.nonstop.domain.friend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

public class FriendDto {

    @Getter
    @NoArgsConstructor
    @Schema(description = "친구 요청 DTO")
    public static class FriendRequestSendDto {
        @NotNull
        @Schema(description = "요청 대상 사용자 ID", example = "2")
        private Long targetUserId;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @Schema(description = "받은 친구 요청 응답 DTO")
    public static class FriendRequestResponseDto {
        @Schema(description = "친구 요청 ID", example = "1")
        private Long requestId;

        @Schema(description = "요청한 사용자 정보")
        private UserInfoDto requester;

        @Schema(description = "요청 시각", example = "2025-12-21T10:00:00")
        private LocalDateTime requestedAt;
    }

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "친구 목록 응답 DTO")
    public static class FriendResponseDto {
        @Schema(description = "친구 관계 ID (friend_requests 테이블의 ID)", example = "1")
        private Long friendshipId;

        @Schema(description = "친구 사용자 정보")
        private UserInfoDto friend;

        @Schema(description = "친구 관계 수락 시각", example = "2025-12-21T11:00:00")
        private LocalDateTime becameFriendAt;
    }
    
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "공용 사용자 정보 DTO")
    public static class UserInfoDto {
        @Schema(description = "사용자 ID", example = "1")
        private Long userId;

        @Schema(description = "닉네임", example = "코알라")
        private String nickname;

        @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
        private String profileImageUrl;

        // University and Major could be added if needed
    }

    @Getter
    @NoArgsConstructor
    @Schema(description = "사용자 차단 요청 DTO")
    public static class UserBlockRequestDto {
        @NotNull
        @Schema(description = "차단 대상 사용자 ID", example = "3")
        private Long targetUserId;
    }
}

package com.app.nonstop.domain.community.dto;

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
public class PostResponseDto {
    private Long id;
    private Long boardId;
    private String writerNickname;
    private Boolean isWriterAnonymous;
    private String title;
    private String content;
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private Boolean isSecret;
    private Boolean isLiked; // 현재 로그인한 유저가 좋아요를 눌렀는지 여부
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

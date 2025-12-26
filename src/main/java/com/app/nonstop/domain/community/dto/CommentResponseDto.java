package com.app.nonstop.domain.community.dto;

import com.app.nonstop.domain.community.entity.CommentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponseDto {
    private Long id;
    private Long postId;
    private Long upperCommentId;
    private String writerNickname;
    private Boolean isWriterAnonymous;
    private String content;
    private CommentType type;
    private Integer depth;
    private Long likeCount;
    private Boolean isLiked;
    private Boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 대댓글 리스트 (계층형 구조 표현을 위해)
    @Builder.Default
    private List<CommentResponseDto> replies = new ArrayList<>();
}

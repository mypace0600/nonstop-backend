package com.app.nonstop.domain.community.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentRequestDto {
    private Long upperCommentId; // 대댓글일 경우 상위 댓글 ID, 없으면 null
    private String content;
    private Boolean isAnonymous;
}

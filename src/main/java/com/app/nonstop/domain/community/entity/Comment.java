package com.app.nonstop.domain.community.entity;

import com.app.nonstop.global.common.entity.BaseTimeEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class Comment extends BaseTimeEntity {
    private Long id;
    private Long postId;
    private Long userId;
    private Long upperCommentId;
    private String content;
    private CommentType type;
    private Boolean isAnonymous;
    private Integer depth;
    private LocalDateTime deletedAt;
}

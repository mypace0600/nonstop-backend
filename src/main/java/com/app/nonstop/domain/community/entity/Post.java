package com.app.nonstop.domain.community.entity;

import com.app.nonstop.global.common.entity.BaseTimeEntity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class Post extends BaseTimeEntity {
    private Long id;
    private Long boardId;
    private Long userId;
    private String title;
    private String content;
    private Long viewCount = 0L;
    private Boolean isAnonymous;
    private Boolean isSecret;
    private LocalDateTime deletedAt;
}

package com.app.nonstop.domain.community.dto;

import com.app.nonstop.domain.community.entity.CommentType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 댓글 관련 DTO 관리 클래스.
 */
public class CommentDto {

    /**
     * 댓글 작성 요청.
     */
    @Getter
    @Setter
    public static class Request {
        /**
         * 상위 댓글 ID (대댓글인 경우).
         * 최상위 댓글일 경우 null.
         */
        private Long upperCommentId;

        @NotBlank(message = "내용은 필수입니다.")
        private String content;

        private java.util.List<String> imageUrls;

        private Boolean isAnonymous = false;
    }

    /**
     * 댓글 응답 (계층형 구조 포함).
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
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
        private java.util.List<String> imageUrls;

        // 대댓글 리스트
        @Builder.Default
        private List<Response> replies = new ArrayList<>();
    }
}

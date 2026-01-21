package com.app.nonstop.domain.community.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 게시글 관련 DTO 관리 클래스.
 */
public class PostDto {

    /**
     * 게시글 작성 및 수정 요청.
     */
    @Getter
    @Setter
    public static class Request {
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 150, message = "제목은 150자를 초과할 수 없습니다.")
        private String title;

        @NotBlank(message = "내용은 필수입니다.")
        private String content;

        private Boolean isAnonymous = false;

        private Boolean isSecret = false;

        private java.util.List<String> imageUrls;
    }

    /**
     * 게시글 상세 및 목록 응답.
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long boardId;
        private Long writerId;
        private String writerNickname;
        private Boolean isWriterAnonymous;
        private String title;
        private String content;
        private Long viewCount;
        private Long likeCount;
        private Long commentCount;
        private Boolean isSecret;
        private Boolean isLiked; // 현재 로그인한 유저가 좋아요를 눌렀는지 여부
        private Boolean isMine;  // 현재 로그인한 유저가 작성자인지 여부
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private java.util.List<String> imageUrls;
    }
}

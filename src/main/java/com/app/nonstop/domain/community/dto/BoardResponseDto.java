package com.app.nonstop.domain.community.dto;

import com.app.nonstop.domain.community.entity.Board;
import com.app.nonstop.domain.community.entity.BoardType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BoardResponseDto {
    private Long id;
    private String name;
    private String description;
    private BoardType type;
    private Boolean isSecret;
    private LocalDateTime createdAt;

    public static BoardResponseDto from(Board board) {
        return BoardResponseDto.builder()
                .id(board.getId())
                .name(board.getName())
                .description(board.getDescription())
                .type(board.getType())
                .isSecret(board.getIsSecret())
                .createdAt(board.getCreatedAt())
                .build();
    }
}

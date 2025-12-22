package com.app.nonstop.domain.community.dto;

import com.app.nonstop.domain.community.entity.Board;
import com.app.nonstop.domain.community.entity.BoardType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BoardResponseDto {
    private Long id;
    private String name;
    private BoardType type;
    private Boolean isSecret;

    public static BoardResponseDto from(Board board) {
        return BoardResponseDto.builder()
                .id(board.getId())
                .name(board.getName())
                .type(board.getType())
                .isSecret(board.getIsSecret())
                .build();
    }
}

package com.app.nonstop.domain.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileUploadRequestDto {

    @NotBlank(message = "파일 이름은 필수입니다.")
    private String fileName;

    @NotBlank(message = "파일 타입은 필수입니다. (예: image/jpeg)")
    private String contentType;

    @NotNull(message = "파일의 목적은 필수입니다.")
    private FilePurpose purpose;

    private Long targetId; // 게시글 ID 등, 목적에 따라 필요한 ID

    public enum FilePurpose {
        PROFILE_IMAGE,
        BOARD_ATTACHMENT,
        STUDENT_ID_VERIFICATION,
        UNIVERSITY_LOGO
    }
}
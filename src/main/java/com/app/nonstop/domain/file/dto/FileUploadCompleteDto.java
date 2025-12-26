package com.app.nonstop.domain.file.dto;

import com.app.nonstop.domain.file.dto.FileUploadRequestDto.FilePurpose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FileUploadCompleteDto {

    @NotBlank(message = "업로드된 파일의 Blob 경로(URL)는 필수입니다.")
    private String blobUrl;

    @NotBlank(message = "파일의 원본 이름은 필수입니다.")
    private String originalFileName;

    @NotNull(message = "파일의 목적은 필수입니다.")
    private FilePurpose purpose;

    private Long targetId; // 게시글 ID 등, 목적에 따라 필요한 ID
}
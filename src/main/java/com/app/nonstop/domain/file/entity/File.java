package com.app.nonstop.domain.file.entity;

import com.app.nonstop.domain.file.dto.FileUploadRequestDto.FilePurpose;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class File {
    private Long id;
    private Long uploaderId;
    private String targetDomain;
    private Long targetId;
    private FilePurpose purpose;
    private String fileUrl;
    private String originalFileName;
    private String contentType;
    private Long fileSizeBytes;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;

    @Builder
    public File(Long uploaderId, String targetDomain, Long targetId, FilePurpose purpose, String fileUrl, String originalFileName, String contentType, Long fileSizeBytes) {
        this.uploaderId = uploaderId;
        this.targetDomain = targetDomain;
        this.targetId = targetId;
        this.purpose = purpose;
        this.fileUrl = fileUrl;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.fileSizeBytes = fileSizeBytes;
    }
}

package com.app.nonstop.domain.file.entity;

import com.app.nonstop.global.common.entity.BaseTimeEntity;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class File extends BaseTimeEntity {
    private Long id;
    private Long uploaderId;
    private String targetDomain; // 'posts', 'comments', 'users', etc.
    private Long targetId;
    private FilePurpose purpose;
    private String fileUrl;
    private String originalFileName;
    private String contentType;
    private Long fileSizeBytes;
    private LocalDateTime deletedAt;
    
    @Builder
    public File(Long uploaderId, String targetDomain, Long targetId, FilePurpose purpose, String fileUrl, String originalFileName) {
        this.uploaderId = uploaderId;
        this.targetDomain = targetDomain;
        this.targetId = targetId;
        this.purpose = purpose;
        this.fileUrl = fileUrl;
        this.originalFileName = originalFileName;
    }
}
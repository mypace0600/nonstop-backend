package com.app.nonstop.domain.file.service;

import com.app.nonstop.domain.file.dto.FileUploadCompleteDto;
import com.app.nonstop.domain.file.dto.FileUploadRequestDto;

public interface FileService {
    String generateSasUrl(Long userId, FileUploadRequestDto requestDto);
    void processUploadComplete(Long userId, FileUploadCompleteDto completeDto);
}
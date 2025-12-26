package com.app.nonstop.domain.file.service;

import com.app.nonstop.domain.file.dto.FileUploadCompleteDto;
import com.app.nonstop.domain.file.dto.FileUploadRequestDto;
import com.app.nonstop.domain.file.entity.File;
import com.app.nonstop.domain.file.mapper.FileMapper;
import com.app.nonstop.domain.user.mapper.UserMapper;
import com.app.nonstop.infra.blob.BlobStorageUploader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final BlobStorageUploader blobStorageUploader;
    private final FileMapper fileMapper;
    private final UserMapper userMapper;

    @Override
    public String generateSasUrl(Long userId, FileUploadRequestDto requestDto) {
        log.info("Generating SAS URL for user {} and file purpose {}", userId, requestDto.getPurpose());
        String blobName = generateBlobName(userId, requestDto);
        OffsetDateTime expiryTime = OffsetDateTime.now().plusHours(1);
        String sasUrl = blobStorageUploader.generateUserDelegationSas(blobName, requestDto.getContentType(), expiryTime);
        // Note: For a more robust system, we could pre-save file metadata here
        // with a 'PENDING' status and match it on the callback.
        return sasUrl;
    }

    @Override
    @Transactional
    public void processUploadComplete(Long userId, FileUploadCompleteDto completeDto) {
        log.info("Processing file upload complete for user: {}, purpose: {}, blob URL: {}",
                userId, completeDto.getPurpose(), completeDto.getBlobUrl());

        String targetDomain;
        Long targetId;

        switch (completeDto.getPurpose()) {
            case PROFILE_IMAGE:
                targetDomain = "users";
                targetId = userId;
                break;
            case BOARD_ATTACHMENT:
                targetDomain = "posts"; // Or 'boards' depending on the domain model
                targetId = completeDto.getTargetId();
                if (targetId == null) {
                    log.error("Target ID is required for BOARD_ATTACHMENT. User: {}", userId);
                    // In a real scenario, throw a specific exception
                    throw new IllegalArgumentException("게시물 ID가 필요합니다.");
                }
                break;
            case STUDENT_ID_VERIFICATION:
                targetDomain = "student_verification_requests";
                targetId = userId; // Assuming one request per user
                break;
            default:
                log.warn("Unhandled file purpose: {}. Saving with general purpose.", completeDto.getPurpose());
                targetDomain = "general";
                targetId = userId;
        }

        File file = File.builder()
                .uploaderId(userId)
                .targetDomain(targetDomain)
                .targetId(targetId)
                .purpose(completeDto.getPurpose())
                .fileUrl(completeDto.getBlobUrl())
                .originalFileName(completeDto.getOriginalFileName())
                // .contentType(completeDto.getContentType()) // contentType is not in completeDto, might need to add it
                .build();

        fileMapper.save(file);
        log.info("Saved file metadata to 'files' table with ID: {}", file.getId());

        // --- Post-processing based on purpose ---
        if (completeDto.getPurpose() == FileUploadRequestDto.FilePurpose.PROFILE_IMAGE) {
            userMapper.updateProfileImage(userId, completeDto.getBlobUrl());
            log.info("Updated profile image for user: {}", userId);
        }
        
        // In a real implementation for STUDENT_ID_VERIFICATION, you would now create
        // the StudentVerificationRequest entry using this file URL.
        // For example:
        // if (completeDto.getPurpose() == FileUploadRequestDto.FilePurpose.STUDENT_ID_VERIFICATION) {
        //     verificationService.createRequest(userId, completeDto.getBlobUrl());
        // }
    }

    private String generateBlobName(Long userId, FileUploadRequestDto requestDto) {
        // In a real app, consider using UUID for unique filenames to prevent overwrites
        String fileName = requestDto.getFileName();
        switch (requestDto.getPurpose()) {
            case PROFILE_IMAGE:
                return String.format("profile-images/%d/%s", userId, fileName);
            case BOARD_ATTACHMENT:
                Long targetId = requestDto.getTargetId();
                if (targetId == null) {
                    throw new IllegalArgumentException("Target ID is required for BOARD_ATTACHMENT blob name generation.");
                }
                return String.format("board-attachments/%d/%s", targetId, fileName);
            case STUDENT_ID_VERIFICATION:
                return String.format("student-verification/%d/%s", userId, fileName);
            default:
                return String.format("general/%d/%s", userId, fileName);
        }
    }
}
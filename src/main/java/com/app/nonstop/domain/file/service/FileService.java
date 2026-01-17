package com.app.nonstop.domain.file.service;

import com.app.nonstop.domain.file.dto.FileUploadCompleteDto;
import com.app.nonstop.domain.file.dto.FileUploadRequestDto;
import com.app.nonstop.domain.file.entity.File;
import com.app.nonstop.domain.file.entity.FilePurpose;
import com.app.nonstop.global.config.AzureBlobStorageConfig;
import com.app.nonstop.mapper.FileMapper;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.sas.BlobSasPermission;
import com.azure.storage.blob.sas.BlobServiceSasSignatureValues;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileMapper fileMapper;
    private final BlobServiceClient blobServiceClient;
    private final AzureBlobStorageConfig azureBlobStorageConfig;

    /**
     * 이미지 URL 리스트를 DB에 저장합니다.
     */
    @Transactional
    public void saveImages(Long uploaderId, String targetDomain, Long targetId, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return;
        }

        List<File> files = imageUrls.stream()
                .map(url -> File.builder()
                        .uploaderId(uploaderId)
                        .targetDomain(targetDomain)
                        .targetId(targetId)
                        .purpose(FilePurpose.BOARD_ATTACHMENT) // 기본값 게시판 첨부
                        .fileUrl(url)
                        .build())
                .collect(Collectors.toList());

        fileMapper.insertAll(files);
    }

    /**
     * 대상(게시글/댓글)의 이미지를 조회하여 URL 리스트로 반환합니다.
     */
    @Transactional(readOnly = true)
    public List<String> getImageUrls(String targetDomain, Long targetId) {
        return fileMapper.findByTarget(targetDomain, targetId).stream()
                .map(File::getFileUrl)
                .collect(Collectors.toList());
    }

    /**
     * 여러 대상의 이미지를 조회하여 Map<TargetId, List<Url>> 형태로 반환합니다.
     */
    @Transactional(readOnly = true)
    public java.util.Map<Long, List<String>> getImageUrlsByTargetIds(String targetDomain, List<Long> targetIds) {
        if (targetIds == null || targetIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        List<File> files = fileMapper.findByTargetIds(targetDomain, targetIds);
        return files.stream()
                .collect(Collectors.groupingBy(
                        File::getTargetId,
                        Collectors.mapping(File::getFileUrl, Collectors.toList())
                ));
    }

    /**
     * 대상의 기존 이미지를 모두 삭제(Soft Delete)하고 새로 저장합니다 (수정 시 사용).
     */
    @Transactional
    public void updateImages(Long uploaderId, String targetDomain, Long targetId, List<String> imageUrls) {
        // 기존 이미지 삭제 처리
        fileMapper.deleteByTarget(targetDomain, targetId);

        // 새 이미지 저장
        saveImages(uploaderId, targetDomain, targetId, imageUrls);
    }

    /**
     * SAS URL 생성
     * 클라이언트가 Azure Blob Storage에 직접 업로드할 수 있도록 일회성 권한 URL을 생성합니다.
     */
    public String generateSasUrl(Long userId, FileUploadRequestDto requestDto) {
        // 1. 설정된 단일 컨테이너 이름 가져오기
        String containerName = azureBlobStorageConfig.getContainerName();
        
        // 2. Blob 컨테이너 클라이언트 가져오기
        BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
        if (!containerClient.exists()) {
            containerClient.create();
        }

        // 3. 파일명 생성 (Purpose를 폴더 구조로 활용 + UUID 조합)
        String directoryPrefix = requestDto.getPurpose().name().toLowerCase();
        String fileName = directoryPrefix + "/" + UUID.randomUUID() + "_" + requestDto.getFileName();
        BlobClient blobClient = containerClient.getBlobClient(fileName);

        // 4. SAS 권한 및 만료 시간 설정 (10분간 쓰기 권한)
        BlobSasPermission permission = new BlobSasPermission().setWritePermission(true);
        OffsetDateTime expiryTime = OffsetDateTime.now().plusMinutes(10);
        
        BlobServiceSasSignatureValues values = new BlobServiceSasSignatureValues(expiryTime, permission)
                .setProtocol(com.azure.storage.common.sas.SasProtocol.HTTPS_ONLY);

        // 5. SAS URL 생성 및 반환
        return blobClient.getBlobUrl() + "?" + blobClient.generateSas(values);
    }

    /**
     * 업로드 완료 처리
     * 클라이언트가 업로드를 마친 후 메타데이터를 저장합니다.
     */
    @Transactional
    public void processUploadComplete(Long userId, FileUploadCompleteDto completeDto) {
        FilePurpose purpose = FilePurpose.valueOf(completeDto.getPurpose().name());
        
        // TargetDomain 결정 (Purpose에 따라 매핑)
        String targetDomain;
        switch (purpose) {
            case PROFILE_IMAGE:
                targetDomain = "USER";
                break;
            case BOARD_ATTACHMENT:
                targetDomain = "POST";
                break;
            case CHAT_IMAGE:
                targetDomain = "CHAT";
                break;
            case STUDENT_ID_CARD:
            case STUDENT_ID_VERIFICATION:
                targetDomain = "VERIFICATION";
                break;
            default:
                targetDomain = "ETC";
        }
        
        File file = File.builder()
                .uploaderId(userId)
                .targetDomain(targetDomain)
                .targetId(completeDto.getTargetId() != null ? completeDto.getTargetId() : 0L)
                .purpose(purpose)
                .fileUrl(completeDto.getBlobUrl())
                .originalFileName(completeDto.getOriginalFileName())
                .build();
        
        fileMapper.insert(file);
    }
}

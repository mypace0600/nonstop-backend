package com.app.nonstop.domain.file.service;

import com.app.nonstop.domain.file.dto.FileUploadCompleteDto;
import com.app.nonstop.domain.file.dto.FileUploadRequestDto;
import com.app.nonstop.domain.file.entity.File;
import com.app.nonstop.domain.file.entity.FilePurpose;
import com.app.nonstop.mapper.FileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileService {

    private final FileMapper fileMapper;

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
     * SAS URL 생성 (Mock)
     */
    public String generateSasUrl(Long userId, FileUploadRequestDto requestDto) {
        // TODO: 실제 Azure Blob SAS URL 생성 로직 구현
        return "https://mock-azure-blob-url.com/" + requestDto.getFileName() + "?sasToken=mock";
    }

    /**
     * 업로드 완료 처리 (Mock: DB 저장)
     */
    @Transactional
    public void processUploadComplete(Long userId, FileUploadCompleteDto completeDto) {
        // targetDomain 결정 로직 필요 (Purpose에 따라 다름)
        // 일단 단순하게 매핑하거나, purpose를 저장
        FilePurpose entityPurpose = FilePurpose.valueOf(completeDto.getPurpose().name());
        
        File file = File.builder()
                .uploaderId(userId)
                .targetDomain("UNKNOWN") // Purpose를 보고 결정해야 함
                .targetId(completeDto.getTargetId() != null ? completeDto.getTargetId() : 0L)
                .purpose(entityPurpose)
                .fileUrl(completeDto.getBlobUrl())
                .originalFileName(completeDto.getOriginalFileName())
                .build();
        
        fileMapper.insert(file);
    }
}

package com.app.nonstop.infra.blob;

import com.app.nonstop.global.common.exception.FileUploadException;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlobStorageUploader {

    private final BlobServiceClient blobServiceClient;

    @Value("${spring.cloud.azure.storage.blob.container-name}")
    private String containerName;

    /**
     * MultipartFile을 Azure Blob Storage에 업로드하고 해당 파일의 URL을 반환합니다.
     *
     * @param multipartFile 업로드할 파일
     * @param dirName       Blob Storage 컨테이너 내에서 파일을 저장할 디렉터리 이름
     * @return 업로드된 파일의 전체 URL
     * @throws FileUploadException 파일 업로드 실패 시
     */
    public String upload(MultipartFile multipartFile, String dirName) {
        if (multipartFile.isEmpty()) {
            throw new FileUploadException("업로드할 파일이 없습니다.");
        }

        String originalFilename = multipartFile.getOriginalFilename();
        String storedFileName = createStoredFileName(originalFilename);
        String fileName = dirName + "/" + storedFileName;

        try {
            BlobContainerClient containerClient = blobServiceClient.getBlobContainerClient(containerName);
            BlobClient blobClient = containerClient.getBlobClient(fileName);

            blobClient.upload(multipartFile.getInputStream(), multipartFile.getSize(), true);

            return blobClient.getBlobUrl();

        } catch (IOException e) {
            log.error("Azure Blob Storage 파일 업로드 중 입출력 오류가 발생했습니다. 파일: {}", originalFilename, e);
            throw new FileUploadException("파일 업로드에 실패했습니다.", e);
        }
    }

    /**
     * 저장될 고유한 파일 이름을 생성합니다.
     *
     * @param originalFilename 원본 파일 이름
     * @return UUID와 원본 파일 확장자를 조합한 새로운 파일 이름
     */
    private String createStoredFileName(String originalFilename) {
        String ext = extractExt(originalFilename);
        String uuid = UUID.randomUUID().toString();
        return uuid + "." + ext;
    }

    /**
     * 원본 파일 이름에서 확장자를 추출합니다.
     *
     * @param originalFilename 원본 파일 이름
     * @return 파일 확장자
     */
    private String extractExt(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int pos = originalFilename.lastIndexOf(".");
        if (pos == -1 || pos == originalFilename.length() - 1) {
            log.warn("확장자가 없는 파일이 업로드되었습니다: {}", originalFilename);
            return "";
        }
        return originalFilename.substring(pos + 1);
    }
}

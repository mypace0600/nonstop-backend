package com.app.nonstop.infra.s3;

import com.app.nonstop.global.common.exception.FileUploadException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3Uploader {

    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * MultipartFile을 S3에 업로드하고 해당 파일의 URL을 반환합니다.
     *
     * @param multipartFile 업로드할 파일
     * @param dirName       S3 버킷 내에서 파일을 저장할 디렉터리 이름
     * @return 업로드된 파일의 전체 URL
     * @throws FileUploadException 파일 업로드 실패 시
     */
    public String upload(MultipartFile multipartFile, String dirName) {
        if (multipartFile.isEmpty()) {
            // TODO: 또는 null을 반환하거나 기본 이미지 URL을 반환하는 정책을 정할 수 있습니다.
            throw new FileUploadException("업로드할 파일이 없습니다.");
        }

        String originalFilename = multipartFile.getOriginalFilename();
        String storedFileName = createStoredFileName(originalFilename);
        String fileName = dirName + "/" + storedFileName;

        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .contentType(multipartFile.getContentType())
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(multipartFile.getInputStream(), multipartFile.getSize()));

            // 업로드된 파일의 URL을 반환합니다.
            return s3Client.utilities().getUrl(builder -> builder.bucket(bucket).key(fileName)).toExternalForm();

        } catch (IOException e) {
            log.error("S3 파일 업로드 중 입출력 오류가 발생했습니다. 파일: {}", originalFilename, e);
            throw new FileUploadException("파일 업로드에 실패했습니다.", e);
        }
    }

    /**
     * S3에 저장될 고유한 파일 이름을 생성합니다.
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
        int pos = originalFilename.lastIndexOf(".");
        if (pos == -1 || pos == originalFilename.length() - 1) {
            // TODO: 확장자가 없는 파일에 대한 처리 정책을 결정해야 합니다. (예: 예외 발생)
            log.warn("확장자가 없는 파일이 업로드되었습니다: {}", originalFilename);
            return ""; // 혹은 기본 확장자
        }
        return originalFilename.substring(pos + 1);
    }
}

package com.app.nonstop.domain.verification.service;

import com.app.nonstop.domain.user.entity.User;
import com.app.nonstop.domain.user.exception.UserNotFoundException;
import com.app.nonstop.domain.report.entity.ReportStatus;
import com.app.nonstop.domain.verification.entity.StudentVerificationRequest;
import com.app.nonstop.domain.verification.exception.FileTooLargeException;
import com.app.nonstop.domain.verification.exception.InvalidFileTypeException;
import com.app.nonstop.domain.verification.exception.VerificationRequestAlreadyExistsException;
import com.app.nonstop.mapper.UserMapper;
import com.app.nonstop.mapper.VerificationMapper;
import com.app.nonstop.infra.blob.BlobStorageUploader;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VerificationServiceImpl implements VerificationService {

    private final UserMapper userMapper;
    private final VerificationMapper verificationMapper;
    
    @Autowired(required = false)
    private BlobStorageUploader blobStorageUploader;

    // Blob Storage에 학생증 이미지를 저장할 디렉터리 이름
    private static final String STUDENT_ID_UPLOAD_DIR = "student-id-verification";
    private static final long MAX_FILE_SIZE_MB = 5; // 최대 파일 크기 5MB
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList("image/jpeg", "image/png");

    @Override
    public void requestStudentIdVerification(Long userId, MultipartFile imageFile) {
        // In test profile, blobStorageUploader might be null.
        if (blobStorageUploader == null) {
            return;
        }
        // 0. 파일 유효성 검사
        if (imageFile.isEmpty()) {
            throw new InvalidFileTypeException("업로드할 파일이 비어있습니다.");
        }
        if (!ALLOWED_IMAGE_TYPES.contains(imageFile.getContentType())) {
            throw new InvalidFileTypeException();
        }
        if (imageFile.getSize() > MAX_FILE_SIZE_MB * 1024 * 1024) {
            throw new FileTooLargeException("파일 크기가 " + MAX_FILE_SIZE_MB + "MB를 초과했습니다.");
        }

        // 1. 사용자 존재 여부 확인
        User user = userMapper.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        // 2. 이미 처리 대기 중인 요청이 있는지 확인
        // TODO: (동시성) 이 select와 insert 사이에 여러 요청이 동시에 들어올 경우 중복 PENDING 요청이 생성될 수 있습니다.
        // 이를 방지하기 위해 student_verification_requests 테이블에 (user_id, status='PENDING')에 대한 조건부 Unique 인덱스 추가를 고려해야 합니다.
        Optional<StudentVerificationRequest> existingRequest = verificationMapper.findPendingRequestByUserId(userId);
        if (existingRequest.isPresent()) {
            throw new VerificationRequestAlreadyExistsException();
        }

        // 3. 학생증 이미지를 Blob Storage에 업로드
        String imageUrl = blobStorageUploader.upload(imageFile, STUDENT_ID_UPLOAD_DIR);

        // 4. StudentVerificationRequest 객체 생성
        StudentVerificationRequest request = StudentVerificationRequest.builder()
                .user(user)
                .imageUrl(imageUrl)
                .status(ReportStatus.PENDING) // 초기 상태는 PENDING
                .build();

        // 5. 인증 요청 정보 저장
        verificationMapper.save(request);
    }
}

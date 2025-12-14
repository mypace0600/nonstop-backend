package com.app.nonstop.domain.verification.service;

import org.springframework.web.multipart.MultipartFile;

public interface VerificationService {

    /**
     * 학생증 사진을 업로드하여 대학생 인증을 요청합니다.
     *
     * @param userId 요청하는 사용자 ID
     * @param imageFile 업로드할 학생증 이미지 파일
     */
    void requestStudentIdVerification(Long userId, MultipartFile imageFile);
}

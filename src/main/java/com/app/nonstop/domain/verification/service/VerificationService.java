package com.app.nonstop.domain.verification.service;

import com.app.nonstop.domain.verification.dto.EmailVerificationConfirmDto;
import com.app.nonstop.domain.verification.dto.EmailVerificationRequestDto;
import org.springframework.web.multipart.MultipartFile;

public interface VerificationService {

    /**
     * 학생증 사진을 업로드하여 대학생 인증을 요청합니다.
     *
     * @param userId 요청하는 사용자 ID
     * @param imageFile 업로드할 학생증 이미지 파일
     */
    void requestStudentIdVerification(Long userId, MultipartFile imageFile);

    /**
     * 학교 웹메일로 인증 코드를 발송합니다.
     * @param userId 요청 사용자 ID
     * @param requestDto 이메일 정보
     */
    void requestEmailVerification(Long userId, EmailVerificationRequestDto requestDto);

    /**
     * 발송된 인증 코드를 검증하고 인증을 완료합니다.
     * @param userId 요청 사용자 ID
     * @param confirmDto 인증 코드 정보
     */
    void confirmEmailVerification(Long userId, EmailVerificationConfirmDto confirmDto);
}

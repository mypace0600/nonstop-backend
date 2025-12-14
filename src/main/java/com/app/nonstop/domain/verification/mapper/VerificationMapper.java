package com.app.nonstop.domain.verification.mapper;

import com.app.nonstop.domain.verification.entity.StudentVerificationRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface VerificationMapper {

    /**
     * 특정 사용자의 처리 대기중(PENDING)인 학생증 인증 요청을 조회합니다.
     *
     * @param userId 조회할 사용자 ID
     * @return Optional<StudentVerificationRequest>
     */
    Optional<StudentVerificationRequest> findPendingRequestByUserId(@Param("userId") Long userId);

    /**
     * 학생증 인증 요청 정보를 저장합니다.
     *
     * @param request 저장할 인증 요청 객체
     */
    void save(StudentVerificationRequest request);

}

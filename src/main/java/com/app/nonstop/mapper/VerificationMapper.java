package com.app.nonstop.mapper;

import com.app.nonstop.domain.verification.entity.StudentVerificationRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface VerificationMapper {

    Optional<StudentVerificationRequest> findPendingRequestByUserId(@Param("userId") Long userId);

    void save(StudentVerificationRequest request);
}

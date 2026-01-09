package com.app.nonstop.mapper;

import com.app.nonstop.domain.major.entity.Major;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface MajorMapper {

    List<Major> findByUniversityId(@Param("universityId") Long universityId);

    List<Major> findByUniversityIdAndKeyword(
            @Param("universityId") Long universityId,
            @Param("keyword") String keyword
    );

    Optional<Major> findById(@Param("id") Long id);

    boolean existsByIdAndUniversityId(@Param("id") Long id, @Param("universityId") Long universityId);
}

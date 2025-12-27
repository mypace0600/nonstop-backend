package com.app.nonstop.mapper;

import com.app.nonstop.domain.file.entity.File;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FileMapper {
    void save(File file);
    // 단건 저장
    void insert(File file);

    // 다건 저장 (Batch Insert)
    void insertAll(@Param("files") List<File> files);

    // 타겟별 파일 조회
    List<File> findByTarget(@Param("targetDomain") String targetDomain, @Param("targetId") Long targetId);

    // 여러 타겟 파일 조회 (IN 절)
    List<File> findByTargetIds(@Param("targetDomain") String targetDomain, @Param("targetIds") List<Long> targetIds);

    // 타겟별 파일 Soft Delete
    void deleteByTarget(@Param("targetDomain") String targetDomain, @Param("targetId") Long targetId);
}

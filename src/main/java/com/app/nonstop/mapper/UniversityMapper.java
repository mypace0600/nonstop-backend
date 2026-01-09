package com.app.nonstop.mapper;

import com.app.nonstop.domain.university.entity.University;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface UniversityMapper {

    List<University> findAll();

    List<University> findByKeyword(@Param("keyword") String keyword);

    List<University> findByRegion(@Param("region") String region);

    List<University> findByKeywordAndRegion(@Param("keyword") String keyword, @Param("region") String region);

    Optional<University> findById(@Param("id") Long id);

    List<String> findAllRegions();
}

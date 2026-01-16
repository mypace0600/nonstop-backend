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

    void updateUniversity(@Param("userId") Long userId, @Param("universityId") Long universityId, @Param("majorId") Long majorId);

    Optional<University> findByDomain(@Param("domain") String domain);

    // 페이징 지원 쿼리
    List<University> findAllWithPaging(@Param("keyword") String keyword,
                                       @Param("region") String region,
                                       @Param("limit") Integer limit,
                                       @Param("offset") Integer offset);

    long countAll(@Param("keyword") String keyword, @Param("region") String region);
}

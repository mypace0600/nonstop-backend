package com.app.nonstop.mapper;

import com.app.nonstop.domain.community.entity.Community;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

import java.util.Optional;

@Mapper
public interface CommunityMapper {
    List<Community> findByUniversityId(@Param("universityId") Long universityId);
    Optional<Community> findById(Long id);
}

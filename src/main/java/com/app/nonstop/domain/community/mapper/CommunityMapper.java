package com.app.nonstop.domain.community.mapper;

import com.app.nonstop.domain.community.entity.Community;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommunityMapper {
    List<Community> findByUniversityId(@Param("universityId") Long universityId);
}

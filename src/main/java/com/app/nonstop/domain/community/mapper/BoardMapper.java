package com.app.nonstop.domain.community.mapper;

import com.app.nonstop.domain.community.entity.Board;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface BoardMapper {
    List<Board> findByCommunityId(@Param("communityId") Long communityId);
}

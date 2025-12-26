package com.app.nonstop.domain.report.mapper;

import com.app.nonstop.domain.report.entity.Report;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ReportMapper {
    void insert(Report report);
    
    // 중복 신고 방지용
    boolean existsByReporterAndTarget(
            @Param("reporterId") Long reporterId, 
            @Param("targetType") String targetType, 
            @Param("targetId") Long targetId
    );
}

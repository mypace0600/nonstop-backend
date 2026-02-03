package com.app.nonstop.mapper;

import com.app.nonstop.domain.timetable.entity.Semester;
import com.app.nonstop.domain.timetable.entity.SemesterType;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SemesterMapper {
    List<Semester> findAllByUniversityId(@Param("universityId") Long universityId);
    Semester findById(Long id);

    Semester findByUniversityIdAndYearAndType(
            @Param("universityId") Long universityId,
            @Param("year") Integer year,
            @Param("type") SemesterType type
    );

    void insert(Semester semester);
}

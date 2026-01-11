package com.app.nonstop.mapper;

import com.app.nonstop.domain.timetable.entity.Timetable;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface TimetableMapper {
    void insert(Timetable timetable);
    boolean existsByUserIdAndSemesterId(@Param("userId") Long userId, @Param("semesterId") Long semesterId);
    Optional<Timetable> findById(Long id);
    List<Timetable> findAllByUserId(Long userId);
    void update(Timetable timetable);
    void delete(Long id);
    List<Timetable> findAllPublicByUniversityId(@Param("universityId") Long universityId);
}

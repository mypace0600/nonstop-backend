package com.app.nonstop.mapper;

import com.app.nonstop.domain.timetable.entity.TimetableEntry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

@Mapper
public interface TimetableEntryMapper {
    void insert(TimetableEntry entry);
    void update(TimetableEntry entry);
    void delete(Long id);
    List<TimetableEntry> findAllByTimetableId(Long timetableId);
    Optional<TimetableEntry> findById(Long id);
}

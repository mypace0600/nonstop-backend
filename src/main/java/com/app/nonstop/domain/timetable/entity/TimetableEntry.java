package com.app.nonstop.domain.timetable.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimetableEntry {
    private Long id;
    private Long timetableId;
    private String subjectName;
    private String professor;
    private String dayOfWeek; // MONDAY, TUESDAY...
    private LocalTime startTime;
    private LocalTime endTime;
    private String place;
    private String color;
}

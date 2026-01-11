package com.app.nonstop.domain.timetable.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Semester {
    private Long id;
    private Long universityId;
    private Integer year;
    private SemesterType type;
    private LocalDateTime createdAt;
}

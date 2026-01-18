package com.app.nonstop.domain.timetable.dto;

import com.app.nonstop.domain.timetable.entity.Semester;
import com.app.nonstop.domain.timetable.entity.SemesterType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class SemesterDto {

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Integer year;
        private SemesterType type;
        private Boolean isCurrent;

        public static Response from(Semester semester) {
            return Response.builder()
                    .id(semester.getId())
                    .year(semester.getYear())
                    .type(semester.getType())
                    .isCurrent(false)
                    .build();
        }

        public static Response from(Semester semester, boolean isCurrent) {
            return Response.builder()
                    .id(semester.getId())
                    .year(semester.getYear())
                    .type(semester.getType())
                    .isCurrent(isCurrent)
                    .build();
        }
    }
}

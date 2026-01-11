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

        public static Response from(Semester semester) {
            return Response.builder()
                    .id(semester.getId())
                    .year(semester.getYear())
                    .type(semester.getType())
                    .build();
        }
    }
}

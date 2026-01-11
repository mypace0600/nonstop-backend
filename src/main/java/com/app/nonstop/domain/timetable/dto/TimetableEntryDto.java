package com.app.nonstop.domain.timetable.dto;

import com.app.nonstop.domain.timetable.entity.DayOfWeek;
import com.app.nonstop.domain.timetable.entity.TimetableEntry;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

public class TimetableEntryDto {

    @Getter
    @Setter
    public static class Request {
        @NotNull
        private String subjectName;
        private String professor;
        @NotNull
        private DayOfWeek dayOfWeek;
        @NotNull
        private LocalTime startTime;
        @NotNull
        private LocalTime endTime;
        private String place;
        private String color;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long timetableId;
        private String subjectName;
        private String professor;
        private DayOfWeek dayOfWeek;
        private LocalTime startTime;
        private LocalTime endTime;
        private String place;
        private String color;

        public static Response from(TimetableEntry entry) {
            return Response.builder()
                    .id(entry.getId())
                    .timetableId(entry.getTimetableId())
                    .subjectName(entry.getSubjectName())
                    .professor(entry.getProfessor())
                    .dayOfWeek(DayOfWeek.valueOf(entry.getDayOfWeek()))
                    .startTime(entry.getStartTime())
                    .endTime(entry.getEndTime())
                    .place(entry.getPlace())
                    .color(entry.getColor())
                    .build();
        }
    }
}

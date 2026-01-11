package com.app.nonstop.domain.timetable.dto;

import com.app.nonstop.domain.timetable.entity.SemesterType;
import com.app.nonstop.domain.timetable.entity.Timetable;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

public class TimetableDto {

    @Getter
    @Setter
    public static class Request {
        // Create only
        private Long semesterId;
        
        // Create & Update
        private String title;
        private Boolean isPublic;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long semesterId;
        private Integer year;
        private SemesterType semesterType;
        private String title;
        private Boolean isPublic;

        public static Response from(Timetable timetable, Integer year, SemesterType type) {
            return Response.builder()
                    .id(timetable.getId())
                    .semesterId(timetable.getSemesterId())
                    .year(year)
                    .semesterType(type)
                    .title(timetable.getTitle())
                    .isPublic(timetable.getIsPublic())
                    .build();
        }
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class DetailResponse extends Response {
        private List<TimetableEntryDto.Response> entries;

        public DetailResponse(Response response, List<TimetableEntryDto.Response> entries) {
            super(response.getId(), response.getSemesterId(), response.getYear(), response.getSemesterType(), response.getTitle(), response.getIsPublic());
            this.entries = entries;
        }
    }
}

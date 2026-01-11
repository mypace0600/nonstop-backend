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
public class Timetable {
    private Long id;
    private Long userId;
    private Long semesterId;
    private String title;
    private Boolean isPublic;
    private LocalDateTime createdAt;
}

package com.app.nonstop.domain.timetable.controller;

import com.app.nonstop.domain.timetable.dto.SemesterDto;
import com.app.nonstop.domain.timetable.dto.TimetableDto;
import com.app.nonstop.domain.timetable.dto.TimetableEntryDto;
import com.app.nonstop.domain.timetable.service.TimetableService;
import com.app.nonstop.global.common.response.ApiResponse;
import com.app.nonstop.global.security.user.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TimetableController {

    private final TimetableService timetableService;

    @GetMapping("/semesters")
    public ApiResponse<List<SemesterDto.Response>> getSemesters(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long universityId = (userDetails != null) ? userDetails.getUniversityId() : null;
        return ApiResponse.success(timetableService.getSemesters(universityId));
    }

    @GetMapping("/timetables")
    public ApiResponse<List<TimetableDto.Response>> getMyTimetables(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.success(timetableService.getMyTimetables(userDetails.getUserId()));
    }

    @PostMapping("/timetables")
    public ApiResponse<TimetableDto.Response> createTimetable(
            @RequestBody @Valid TimetableDto.Request request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.success(timetableService.createTimetable(userDetails.getUserId(), request));
    }

    @GetMapping("/timetables/{id}")
    public ApiResponse<TimetableDto.DetailResponse> getTimetableDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.success(timetableService.getTimetableDetail(userDetails.getUserId(), id));
    }

    @PatchMapping("/timetables/{id}")
    public ApiResponse<TimetableDto.Response> updateTimetable(
            @PathVariable Long id,
            @RequestBody @Valid TimetableDto.Request request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.success(timetableService.updateTimetable(userDetails.getUserId(), id, request));
    }

    @DeleteMapping("/timetables/{id}")
    public ApiResponse<?> deleteTimetable(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        timetableService.deleteTimetable(userDetails.getUserId(), id);
        return ApiResponse.success();
    }

    @PostMapping("/timetables/{id}/entries")
    public ApiResponse<TimetableEntryDto.Response> addEntry(
            @PathVariable Long id,
            @RequestBody @Valid TimetableEntryDto.Request request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.success(timetableService.addEntry(userDetails.getUserId(), id, request));
    }

    @PatchMapping("/timetables/entries/{id}")
    public ApiResponse<TimetableEntryDto.Response> updateEntry(
            @PathVariable Long id,
            @RequestBody @Valid TimetableEntryDto.Request request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.success(timetableService.updateEntry(userDetails.getUserId(), id, request));
    }

    @DeleteMapping("/timetables/entries/{id}")
    public ApiResponse<?> deleteEntry(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        timetableService.deleteEntry(userDetails.getUserId(), id);
        return ApiResponse.success();
    }

    @GetMapping("/timetables/public")
    public ApiResponse<List<TimetableDto.Response>> getPublicTimetables(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.success(timetableService.getPublicTimetables(
                userDetails.getUniversityId(),
                userDetails.getIsUniversityVerified()
        ));
    }
}

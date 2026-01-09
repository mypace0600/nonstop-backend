package com.app.nonstop.domain.university.controller;

import com.app.nonstop.domain.university.dto.MajorResponseDto;
import com.app.nonstop.domain.university.dto.UniversityResponseDto;
import com.app.nonstop.domain.university.service.UniversityService;
import com.app.nonstop.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/universities")
@RequiredArgsConstructor
public class UniversityController {

    private final UniversityService universityService;

    /**
     * 대학교 목록 조회
     * @param keyword 검색어 (선택)
     * @param region 지역 필터 (선택)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<UniversityResponseDto>>> getUniversities(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String region
    ) {
        List<UniversityResponseDto> universities = universityService.getUniversities(keyword, region);
        return ResponseEntity.ok(ApiResponse.success(universities));
    }

    /**
     * 대학교 상세 조회
     */
    @GetMapping("/{universityId}")
    public ResponseEntity<ApiResponse<UniversityResponseDto>> getUniversity(
            @PathVariable Long universityId
    ) {
        UniversityResponseDto university = universityService.getUniversity(universityId);
        return ResponseEntity.ok(ApiResponse.success(university));
    }

    /**
     * 대학교별 전공 목록 조회
     * @param universityId 대학교 ID
     * @param keyword 검색어 (선택)
     */
    @GetMapping("/{universityId}/majors")
    public ResponseEntity<ApiResponse<List<MajorResponseDto>>> getMajors(
            @PathVariable Long universityId,
            @RequestParam(required = false) String keyword
    ) {
        List<MajorResponseDto> majors = universityService.getMajorsByUniversity(universityId, keyword);
        return ResponseEntity.ok(ApiResponse.success(majors));
    }

    /**
     * 지역 목록 조회 (필터링용)
     */
    @GetMapping("/regions")
    public ResponseEntity<ApiResponse<List<String>>> getRegions() {
        List<String> regions = universityService.getAllRegions();
        return ResponseEntity.ok(ApiResponse.success(regions));
    }
}

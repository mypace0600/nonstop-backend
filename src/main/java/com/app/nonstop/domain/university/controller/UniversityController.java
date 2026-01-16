package com.app.nonstop.domain.university.controller;

import com.app.nonstop.domain.university.dto.MajorResponseDto;
import com.app.nonstop.domain.university.dto.UniversityResponseDto;
import com.app.nonstop.domain.university.service.UniversityService;
import com.app.nonstop.global.common.response.ApiResponse;
import com.app.nonstop.global.common.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "University", description = "대학교 관련 API")
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
     * 대학교 전체 목록 조회 (회원가입용, 페이징 지원)
     * 인증 없이 접근 가능
     */
    @Operation(
            summary = "대학교 전체 목록 조회 (회원가입용)",
            description = "회원가입 시 대학교 선택을 위한 API입니다. 인증 없이 접근 가능하며, 페이징을 지원합니다."
    )
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<PagedResponse<UniversityResponseDto>>> getUniversityList(
            @Parameter(description = "검색어 (대학교 이름)") @RequestParam(required = false) String keyword,
            @Parameter(description = "지역 필터") @RequestParam(required = false) String region,
            @Parameter(description = "한 페이지당 개수 (미입력시 전체 조회)") @RequestParam(required = false) Integer limit,
            @Parameter(description = "시작 위치 (기본값: 0)") @RequestParam(required = false) Integer offset
    ) {
        PagedResponse<UniversityResponseDto> universities = universityService.getUniversitiesWithPaging(keyword, region, limit, offset);
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

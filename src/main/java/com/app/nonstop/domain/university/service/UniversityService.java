package com.app.nonstop.domain.university.service;

import com.app.nonstop.domain.university.dto.MajorResponseDto;
import com.app.nonstop.domain.university.dto.UniversityResponseDto;
import com.app.nonstop.global.common.response.PagedResponse;

import java.util.List;

public interface UniversityService {

    List<UniversityResponseDto> getUniversities(String keyword, String region);

    PagedResponse<UniversityResponseDto> getUniversitiesWithPaging(String keyword, String region, Integer limit, Integer offset);

    UniversityResponseDto getUniversity(Long id);

    List<MajorResponseDto> getMajorsByUniversity(Long universityId, String keyword);

    List<String> getAllRegions();
}

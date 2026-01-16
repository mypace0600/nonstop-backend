package com.app.nonstop.domain.university.service;

import com.app.nonstop.domain.major.entity.Major;
import com.app.nonstop.domain.university.dto.MajorResponseDto;
import com.app.nonstop.domain.university.dto.UniversityResponseDto;
import com.app.nonstop.domain.university.entity.University;
import com.app.nonstop.global.common.exception.ResourceNotFoundException;
import com.app.nonstop.global.common.response.PagedResponse;
import com.app.nonstop.mapper.MajorMapper;
import com.app.nonstop.mapper.UniversityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UniversityServiceImpl implements UniversityService {

    private final UniversityMapper universityMapper;
    private final MajorMapper majorMapper;

    @Override
    public List<UniversityResponseDto> getUniversities(String keyword, String region) {
        List<University> universities;

        boolean hasKeyword = StringUtils.hasText(keyword);
        boolean hasRegion = StringUtils.hasText(region);

        if (hasKeyword && hasRegion) {
            universities = universityMapper.findByKeywordAndRegion(keyword, region);
        } else if (hasKeyword) {
            universities = universityMapper.findByKeyword(keyword);
        } else if (hasRegion) {
            universities = universityMapper.findByRegion(region);
        } else {
            universities = universityMapper.findAll();
        }

        return universities.stream()
                .map(UniversityResponseDto::from)
                .collect(Collectors.toList());
    }

    @Override
    public PagedResponse<UniversityResponseDto> getUniversitiesWithPaging(String keyword, String region, Integer limit, Integer offset) {
        List<University> universities = universityMapper.findAllWithPaging(
                StringUtils.hasText(keyword) ? keyword : null,
                StringUtils.hasText(region) ? region : null,
                limit,
                offset
        );

        long totalCount = universityMapper.countAll(
                StringUtils.hasText(keyword) ? keyword : null,
                StringUtils.hasText(region) ? region : null
        );

        List<UniversityResponseDto> items = universities.stream()
                .map(UniversityResponseDto::from)
                .collect(Collectors.toList());

        return PagedResponse.of(items, totalCount, limit, offset);
    }

    @Override
    public UniversityResponseDto getUniversity(Long id) {
        University university = universityMapper.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("University not found: " + id));

        return UniversityResponseDto.from(university);
    }

    @Override
    public List<MajorResponseDto> getMajorsByUniversity(Long universityId, String keyword) {
        // 대학교 존재 여부 확인
        universityMapper.findById(universityId)
                .orElseThrow(() -> new ResourceNotFoundException("University not found: " + universityId));

        List<Major> majors;

        if (StringUtils.hasText(keyword)) {
            majors = majorMapper.findByUniversityIdAndKeyword(universityId, keyword);
        } else {
            majors = majorMapper.findByUniversityId(universityId);
        }

        return majors.stream()
                .map(MajorResponseDto::from)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> getAllRegions() {
        return universityMapper.findAllRegions();
    }
}

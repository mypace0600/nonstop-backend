package com.app.nonstop.domain.timetable.service;

import com.app.nonstop.domain.timetable.dto.SemesterDto;
import com.app.nonstop.domain.timetable.dto.TimetableDto;
import com.app.nonstop.domain.timetable.dto.TimetableEntryDto;
import com.app.nonstop.domain.timetable.entity.Semester;
import com.app.nonstop.domain.timetable.entity.Timetable;
import com.app.nonstop.domain.timetable.entity.TimetableEntry;
import com.app.nonstop.mapper.SemesterMapper;
import com.app.nonstop.mapper.TimetableEntryMapper;
import com.app.nonstop.mapper.TimetableMapper;
import com.app.nonstop.global.common.exception.AccessDeniedException;
import com.app.nonstop.global.common.exception.BusinessException;
import com.app.nonstop.global.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimetableService {

    private final TimetableMapper timetableMapper;
    private final TimetableEntryMapper timetableEntryMapper;
    private final SemesterMapper semesterMapper;

    @Transactional(readOnly = true)
    public List<SemesterDto.Response> getSemesters(Long universityId) {
        if (universityId == null) {
            // If no university, maybe return empty or global?
            // PRD implies "학기별" and university linkage in DB.
            // For now, return empty if no university ID.
            return List.of();
        }
        return semesterMapper.findAllByUniversityId(universityId).stream()
                .map(semester -> SemesterDto.Response.from(semester, isCurrentSemester(semester)))
                .collect(Collectors.toList());
    }

    private boolean isCurrentSemester(Semester semester) {
        java.time.LocalDate now = java.time.LocalDate.now();
        int month = now.getMonthValue();
        int year = now.getYear();

        switch (semester.getType()) {
            case FIRST:
                return year == semester.getYear() && month >= 1 && month <= 7;
                
            case SECOND:
                return year == semester.getYear() && month >= 8 && month <= 12;
                
            default:
                return false;
        }
    }

    @Transactional
    public TimetableDto.Response createTimetable(Long userId, TimetableDto.Request request) {
        if (timetableMapper.existsByUserIdAndSemesterId(userId, request.getSemesterId())) {
            throw new BusinessException("이미 해당 학기의 시간표가 존재합니다.");
        }

        Timetable timetable = Timetable.builder()
                .userId(userId)
                .semesterId(request.getSemesterId())
                .title(request.getTitle())
                .isPublic(request.getIsPublic())
                .build();
        
        timetableMapper.insert(timetable);

        return toResponse(timetable);
    }

    @Transactional(readOnly = true)
    public List<TimetableDto.Response> getMyTimetables(Long userId) {
        return timetableMapper.findAllByUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TimetableDto.DetailResponse getTimetableDetail(Long userId, Long timetableId) {
        Timetable timetable = timetableMapper.findById(timetableId)
                .orElseThrow(() -> new ResourceNotFoundException("시간표를 찾을 수 없습니다."));

        // 본인 시간표가 아니고 비공개인 경우 접근 거부
        if (!timetable.getUserId().equals(userId) && !Boolean.TRUE.equals(timetable.getIsPublic())) {
            throw new AccessDeniedException("비공개 시간표는 본인만 조회할 수 있습니다.");
        }

        List<TimetableEntryDto.Response> entries = timetableEntryMapper.findAllByTimetableId(timetableId).stream()
                .map(TimetableEntryDto.Response::from)
                .collect(Collectors.toList());

        return new TimetableDto.DetailResponse(toResponse(timetable), entries);
    }

    @Transactional
    public TimetableDto.Response updateTimetable(Long userId, Long timetableId, TimetableDto.Request request) {
        Timetable timetable = timetableMapper.findById(timetableId)
                .orElseThrow(() -> new ResourceNotFoundException("시간표를 찾을 수 없습니다."));

        validateTimetableOwnership(timetable, userId, "수정");

        timetable.setTitle(request.getTitle());
        timetable.setIsPublic(request.getIsPublic());
        timetableMapper.update(timetable);

        return toResponse(timetable);
    }

    @Transactional
    public void deleteTimetable(Long userId, Long timetableId) {
        Timetable timetable = timetableMapper.findById(timetableId)
                .orElseThrow(() -> new ResourceNotFoundException("시간표를 찾을 수 없습니다."));

        validateTimetableOwnership(timetable, userId, "삭제");

        // 시간표 삭제 전 수업 항목들 먼저 삭제 (CASCADE 없음)
        timetableEntryMapper.deleteAllByTimetableId(timetableId);

        timetableMapper.delete(timetableId);
    }

    @Transactional
    public TimetableEntryDto.Response addEntry(Long userId, Long timetableId, TimetableEntryDto.Request request) {
        Timetable timetable = timetableMapper.findById(timetableId)
                .orElseThrow(() -> new ResourceNotFoundException("시간표를 찾을 수 없습니다."));

        validateTimetableOwnership(timetable, userId, "수업을 추가");

        validateNoTimeOverlap(timetableId, request, null);

        TimetableEntry entry = TimetableEntry.builder()
                .timetableId(timetableId)
                .subjectName(request.getSubjectName())
                .professor(request.getProfessor())
                .dayOfWeek(request.getDayOfWeek().name())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .place(request.getPlace())
                .color(request.getColor())
                .build();

        timetableEntryMapper.insert(entry);

        return TimetableEntryDto.Response.from(entry);
    }

    @Transactional
    public TimetableEntryDto.Response updateEntry(Long userId, Long entryId, TimetableEntryDto.Request request) {
        TimetableEntry entry = timetableEntryMapper.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("수업을 찾을 수 없습니다."));

        Timetable timetable = timetableMapper.findById(entry.getTimetableId())
                .orElseThrow(() -> new ResourceNotFoundException("시간표를 찾을 수 없습니다."));

        validateTimetableOwnership(timetable, userId, "수업을 수정");

        validateNoTimeOverlap(entry.getTimetableId(), request, entryId);

        entry.setSubjectName(request.getSubjectName());
        entry.setProfessor(request.getProfessor());
        entry.setDayOfWeek(request.getDayOfWeek().name());
        entry.setStartTime(request.getStartTime());
        entry.setEndTime(request.getEndTime());
        entry.setPlace(request.getPlace());
        entry.setColor(request.getColor());

        timetableEntryMapper.update(entry);

        return TimetableEntryDto.Response.from(entry);
    }

    @Transactional
    public void deleteEntry(Long userId, Long entryId) {
        TimetableEntry entry = timetableEntryMapper.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("수업을 찾을 수 없습니다."));

        Timetable timetable = timetableMapper.findById(entry.getTimetableId())
                .orElseThrow(() -> new ResourceNotFoundException("시간표를 찾을 수 없습니다."));

        validateTimetableOwnership(timetable, userId, "수업을 삭제");

        timetableEntryMapper.delete(entryId);
    }

    @Transactional(readOnly = true)
    public List<TimetableDto.Response> getPublicTimetables(Long universityId, Boolean isVerified) {
        if (universityId == null || !Boolean.TRUE.equals(isVerified)) {
            throw new AccessDeniedException("대학 인증이 완료된 사용자만 공개 시간표를 조회할 수 있습니다.");
        }

        return timetableMapper.findAllPublicByUniversityId(universityId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private TimetableDto.Response toResponse(Timetable timetable) {
        Semester semester = semesterMapper.findById(timetable.getSemesterId());
        return TimetableDto.Response.from(timetable, semester != null ? semester.getYear() : null, semester != null ? semester.getType() : null);
    }

    /**
     * 시간표 소유권 검증
     * @param timetable 검증할 시간표
     * @param userId 현재 사용자 ID
     * @param action 수행하려는 동작 (에러 메시지용)
     */
    private void validateTimetableOwnership(Timetable timetable, Long userId, String action) {
        if (!timetable.getUserId().equals(userId)) {
            throw new AccessDeniedException("본인의 시간표에만 " + action + "할 수 있습니다.");
        }
    }

    /**
     * 수업 시간 중복 검증
     * @param timetableId 시간표 ID
     * @param request 수업 요청 정보
     * @param excludeEntryId 제외할 수업 ID (수정 시 자기 자신 제외, 추가 시 null)
     */
    private void validateNoTimeOverlap(Long timetableId, TimetableEntryDto.Request request, Long excludeEntryId) {
        List<TimetableEntry> existingEntries = timetableEntryMapper.findAllByTimetableId(timetableId);

        for (TimetableEntry entry : existingEntries) {
            if (excludeEntryId != null && entry.getId().equals(excludeEntryId)) {
                continue;
            }

            if (entry.getDayOfWeek().equals(request.getDayOfWeek().name())) {
                boolean isOverlap = request.getStartTime().isBefore(entry.getEndTime())
                        && request.getEndTime().isAfter(entry.getStartTime());
                if (isOverlap) {
                    throw new BusinessException("수업 시간이 겹칩니다: " + entry.getSubjectName()
                            + " (" + entry.getStartTime() + " ~ " + entry.getEndTime() + ")");
                }
            }
        }
    }
}

# 학기 자동 생성 구현 가이드

## 개요

시간표 생성 시 해당 학기가 DB에 없으면 자동으로 생성하는 기능입니다.
프론트엔드에서 년도와 학기 타입만 선택하면 백엔드에서 알아서 처리합니다.

---

## 현재 구조

```
TimetableDto.Request
├── semesterId (Long)  ← 현재: 프론트가 semester ID를 알아야 함
├── title
└── isPublic
```

## 변경 후 구조

```
TimetableDto.Request
├── year (Integer)           ← 변경: 년도만 전달
├── semesterType (String)    ← 변경: "FIRST" 또는 "SECOND"
├── title
└── isPublic
```

---

## 구현 단계

### 1. TimetableDto.Request 수정

**파일:** `src/main/java/com/app/nonstop/domain/timetable/dto/TimetableDto.java`

```java
@Getter
@Setter
public static class Request {
    // 기존 semesterId 제거 (또는 deprecated)
    // private Long semesterId;

    // 새로 추가
    @NotNull(message = "년도는 필수입니다")
    private Integer year;

    @NotNull(message = "학기 타입은 필수입니다")
    private SemesterType semesterType;  // FIRST, SECOND

    private String title;
    private Boolean isPublic;
}
```

---

### 2. SemesterMapper 인터페이스 수정

**파일:** `src/main/java/com/app/nonstop/mapper/SemesterMapper.java`

```java
@Mapper
public interface SemesterMapper {
    // 기존 메서드
    List<Semester> findAllByUniversityId(@Param("universityId") Long universityId);
    Semester findById(Long id);

    // 새로 추가
    Semester findByUniversityIdAndYearAndType(
        @Param("universityId") Long universityId,
        @Param("year") Integer year,
        @Param("type") SemesterType type
    );

    void insert(Semester semester);
}
```

---

### 3. SemesterMapper.xml 수정

**파일:** `src/main/resources/mybatis/mappers/timetable/SemesterMapper.xml`

```xml
<!-- 기존 쿼리 아래에 추가 -->

<select id="findByUniversityIdAndYearAndType" resultMap="semesterMap">
    SELECT * FROM semesters
    WHERE university_id = #{universityId}
      AND year = #{year}
      AND type = #{type}::semester_type
</select>

<insert id="insert" useGeneratedKeys="true" keyProperty="id">
    INSERT INTO semesters (university_id, year, type, created_at)
    VALUES (#{universityId}, #{year}, #{type}::semester_type, NOW())
</insert>
```

---

### 4. TimetableService 수정

**파일:** `src/main/java/com/app/nonstop/domain/timetable/service/TimetableService.java`

```java
@Transactional
public TimetableDto.Response createTimetable(Long userId, Long universityId, TimetableDto.Request request) {
    // 1. universityId 검증
    if (universityId == null) {
        throw new BusinessException("대학 인증이 필요합니다.");
    }

    // 2. 학기 조회 또는 생성
    Long semesterId = getOrCreateSemester(universityId, request.getYear(), request.getSemesterType());

    // 3. 중복 시간표 체크 (같은 학기에 이미 시간표가 있는지)
    if (timetableMapper.existsByUserIdAndSemesterId(userId, semesterId)) {
        throw new BusinessException("해당 학기에 이미 시간표가 존재합니다.");
    }

    // 4. 시간표 생성
    Timetable timetable = Timetable.builder()
            .userId(userId)
            .semesterId(semesterId)
            .title(request.getTitle())
            .isPublic(request.getIsPublic())
            .build();

    timetableMapper.insert(timetable);

    return toResponse(timetable);
}

/**
 * 학기 조회 또는 자동 생성
 */
private Long getOrCreateSemester(Long universityId, Integer year, SemesterType type) {
    // 1. 기존 학기 조회
    Semester semester = semesterMapper.findByUniversityIdAndYearAndType(universityId, year, type);

    // 2. 있으면 ID 반환
    if (semester != null) {
        return semester.getId();
    }

    // 3. 없으면 새로 생성
    Semester newSemester = Semester.builder()
            .universityId(universityId)
            .year(year)
            .type(type)
            .build();

    semesterMapper.insert(newSemester);

    return newSemester.getId();
}
```

---

### 5. TimetableMapper 수정 (중복 체크용)

**파일:** `src/main/java/com/app/nonstop/mapper/TimetableMapper.java`

```java
// 추가
boolean existsByUserIdAndSemesterId(@Param("userId") Long userId, @Param("semesterId") Long semesterId);
```

**파일:** `src/main/resources/mybatis/mappers/timetable/TimetableMapper.xml`

```xml
<select id="existsByUserIdAndSemesterId" resultType="boolean">
    SELECT EXISTS (
        SELECT 1 FROM time_tables
        WHERE user_id = #{userId} AND semester_id = #{semesterId}
    )
</select>
```

---

### 6. TimetableController 수정

**파일:** `src/main/java/com/app/nonstop/domain/timetable/controller/TimetableController.java`

```java
@PostMapping
public ResponseEntity<TimetableDto.Response> createTimetable(
        @AuthUser UserPrincipal user,
        @Valid @RequestBody TimetableDto.Request request) {

    // universityId를 UserPrincipal에서 가져옴
    TimetableDto.Response response = timetableService.createTimetable(
        user.getId(),
        user.getUniversityId(),  // 대학 ID 전달
        request
    );

    return ResponseEntity.status(HttpStatus.CREATED).body(response);
}
```

---

## 프론트엔드 변경사항

### 기존 API 요청

```json
POST /api/v1/timetables
{
  "semesterId": 1,
  "title": "내 시간표",
  "isPublic": false
}
```

### 변경 후 API 요청

```json
POST /api/v1/timetables
{
  "year": 2026,
  "semesterType": "FIRST",
  "title": "내 시간표",
  "isPublic": false
}
```

---

## 프론트엔드 UI 예시

```
┌─────────────────────────────────┐
│      시간표 만들기              │
├─────────────────────────────────┤
│  년도:    [2026 ▼]              │
│  학기:    [1학기 ▼]             │
│  제목:    [_______________]     │
│  공개:    [ ] 공개로 설정       │
│                                 │
│         [생성하기]              │
└─────────────────────────────────┘
```

**semesterType 매핑:**
- "1학기" → `FIRST`
- "2학기" → `SECOND`
- "여름 계절학기" → `SUMMER` (선택적)
- "겨울 계절학기" → `WINTER` (선택적)

---

## 학기 목록 조회 API 변경 (선택사항)

현재 `GET /api/v1/semesters`는 DB에 있는 학기만 반환합니다.
프론트에서 년도/학기 선택 드롭다운을 만들려면 두 가지 옵션이 있습니다:

### 옵션 A: 프론트에서 직접 생성 (추천)

프론트에서 현재 년도 기준으로 선택 가능한 년도/학기 목록을 생성합니다.

```javascript
// 예: 현재년도 -2년 ~ 현재년도 +1년
const years = [2024, 2025, 2026, 2027];
const semesters = ['FIRST', 'SECOND'];
```

### 옵션 B: 백엔드 API 추가

가능한 학기 목록을 반환하는 API를 추가합니다.

```
GET /api/v1/semesters/available
→ [
    { "year": 2026, "type": "FIRST", "label": "2026년 1학기" },
    { "year": 2026, "type": "SECOND", "label": "2026년 2학기" },
    ...
  ]
```

---

## 정리

| 변경 파일 | 변경 내용 |
|----------|----------|
| `TimetableDto.java` | Request에 year, semesterType 추가 |
| `SemesterMapper.java` | findByUniversityIdAndYearAndType, insert 추가 |
| `SemesterMapper.xml` | 해당 쿼리 추가 |
| `TimetableService.java` | getOrCreateSemester 로직 추가 |
| `TimetableMapper.java` | existsByUserIdAndSemesterId 추가 (선택) |
| `TimetableMapper.xml` | 해당 쿼리 추가 (선택) |
| `TimetableController.java` | universityId 전달 |

---

## 테스트 시나리오

1. **첫 시간표 생성**: 학기 데이터 없이 시간표 생성 → 학기 자동 생성 확인
2. **같은 학기 재생성**: 동일 년도/학기로 시간표 생성 → 기존 학기 사용 확인
3. **중복 시간표 방지**: 같은 학기에 두 번째 시간표 생성 시도 → 에러 확인
4. **대학 미인증 사용자**: universityId null인 경우 → 에러 확인

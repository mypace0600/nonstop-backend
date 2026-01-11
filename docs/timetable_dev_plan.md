# Timetable Development Plan

## 1. Domain Structure Setup [COMPLETED]
- **Action**: Create the following directory structure:
  - `src/main/java/com/app/nonstop/domain/timetable/controller`
  - `src/main/java/com/app/nonstop/domain/timetable/service`
  - `src/main/java/com/app/nonstop/domain/timetable/entity`
  - `src/main/java/com/app/nonstop/domain/timetable/dto`
  - `src/main/resources/mybatis/mappers/timetable` (for XML mappers)

## 2. Database Schema (Confirmed) [COMPLETED]
- **Tables** (from `docs/db/migrations.sql`):
    - `semesters`: `id`, `university_id`, `year`, `type` (Enum: FIRST, SECOND, SUMMER, WINTER)
    - `time_tables`: `id`, `user_id`, `semester_id`, `title`, `is_public`, `created_at`
        - **Constraint**: `UNIQUE(user_id, semester_id)` (One timetable per semester per user)
    - `time_table_entries`: `id`, `time_table_id`, `subject_name`, `professor`, `day_of_week`, `start_time`, `end_time`, `place`, `color`

## 3. Entity Implementation Details [COMPLETED]
- **`Semester.java`**:
    - Fields: `Long id`, `Long universityId`, `Integer year`, `SemesterType type` (Enum)
    - **Enum**: Create `SemesterType` with values `FIRST`, `SECOND`, `SUMMER`, `WINTER`.
- **`Timetable.java`**:
    - Fields: `Long id`, `Long userId`, `Long semesterId`, `String title`, `Boolean isPublic`, `LocalDateTime createdAt`
- **`TimetableEntry.java`**:
    - Fields: `Long id`, `Long timetableId`, `String subjectName`, `String professor`, `DayOfWeek dayOfWeek` (Enum/String), `LocalTime startTime`, `LocalTime endTime`, `String place`, `String color`

## 4. DTO Implementation Details [COMPLETED]
- **`SemesterDto.Response`**: `id`, `year`, `type`
- **`TimetableDto.Request`**: `semesterId` (for Create), `title`, `isPublic` (for Create/Update)
- **`TimetableDto.Response`**: `id`, `semesterId`, `year`, `semesterType`, `title`, `isPublic`
- **`TimetableDto.DetailResponse`**: Extends `Response` + `List<TimetableEntryDto.Response> entries`
- **`TimetableEntryDto.Request`**: `subjectName`, `professor`, `dayOfWeek`, `startTime`, `endTime`, `place`, `color`
    - **Validation**: `startTime < endTime`, `dayOfWeek` must be valid.
- **`TimetableEntryDto.Response`**: All fields from Entity.

## 5. Mapper (MyBatis) Details [COMPLETED]
- **Pattern**: When inserting/updating Enums, use explicit PostgreSQL casting (e.g., `#{type}::semester_type`).
- **`SemesterMapper`**:
    - `findAllByUniversityId(universityId)`: Retrieve semesters for a specific university.
- **`TimetableMapper`**:
    - `insert(Timetable)`: Create new timetable.
        - **SQL**: `VALUES (..., #{semesterId}, #{title}, #{isPublic}, NOW())` (No Enum here, but watch out if future fields use Enums).
    - `existsByUserIdAndSemesterId(userId, semesterId)`
    - `findById(id)`
    - `findAllByUserId(userId)`
    - `update(Timetable)`
    - `delete(id)`
    - `findAllPublicByUniversityId(universityId)`
- **`TimetableEntryMapper`**:
    - `insert(TimetableEntry)`:
        - **SQL**: `VALUES (..., #{dayOfWeek}, #{startTime}, ...)`
        - **Note**: `day_of_week` in DB is `VARCHAR(20)`, not an Enum type in the schema I reviewed (`docs/db/migrations.sql` says `day_of_week VARCHAR(20)`). **Correction**: Check schema again.
        - *Re-check Schema*: `day_of_week VARCHAR(20)`. So no casting needed, just String.
    - `update(TimetableEntry)`
    - `delete(id)`
    - `findAllByTimetableId(timetableId)`

## 6. Service Logic Details [COMPLETED]
- **`TimetableService`**:
    - **`createTimetable`**: Check `existsByUserIdAndSemesterId`. If true, throw `BusinessException("Already exists for this semester")`.
    - **`addEntry`**:
        - Verify `timetable.userId == currentUserId`.
        - **Conflict Check**: Check if new entry overlaps with existing entries in the same timetable (`dayOfWeek` match && time overlap).
    - **`getPublicTimetables`**:
        - **Security**: If `currentUser.universityId == null` or `!currentUser.isVerified`, throw `AccessDeniedException`.
        - Call `mapper.findAllPublicByUniversityId(currentUser.universityId)`.

## 7. Controller Endpoints [COMPLETED]
- `GET /api/v1/semesters`: Filter by `user.universityId` (if available) or return generic if global (PRD implies university specific in DB).
- `GET /api/v1/timetables`: Returns `List<TimetableDto.Response>` (no entries).
- `GET /api/v1/timetables/{id}`: Returns `TimetableDto.DetailResponse` (with entries).
- `POST /api/v1/timetables`: Body `{ semesterId, title, isPublic }`.
- `POST /api/v1/timetables/{id}/entries`: Body `TimetableEntryDto.Request`.

## 8. Step-by-Step Execution Plan [COMPLETED]
1.  **Setup**: Create packages and `SemesterType` enum.
2.  **Entities & DTOs**: Implement Entity classes and DTO records/classes with Validation annotations.
3.  **Mappers**: Create `Mapper` interfaces and XML files. Write SQL queries.
4.  **Service**: Implement `TimetableService` with business logic (overlap check, ownership check).
5.  **Controller**: Implement `TimetableController` and `SemesterController` (or merge).
6.  **Verify**: Run application, test endpoints with Postman/curl (Create semester -> Create timetable -> Add entries -> Check overlap -> Get Public).

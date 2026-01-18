# Development Progress Report

This document records the recent changes and architectural updates in the **Nonstop** backend.

## 📅 Summary of Changes (2026-01-18)
The recent updates focused on two main areas:
1.  **Academic Calendar Logic**: Implementation of the Uzbekistan-specific semester system.
2.  **Security & Authentication**: Enhancement of the JWT parsing logic and stabilization of the 401 Unauthorized response handling.

---

## 🚀 Commit Details

### 1. Refactor: Update Uzbekistan Academic Calendar Logic
- **Commit Hash**: `124428688ec4b86a57377f6bc005e95761048fc0`
- **Affected Files**:
    - `src/main/java/com/app/nonstop/domain/timetable/service/TimetableService.java`

#### Detailed Changes:
- **Timetable Creation Refactoring**: Removed redundant validation checks during the `createTimetable` process. This simplifies the logic and improves performance when students are setting up their schedules.
- **Service Logic Cleanup**: Streamlined the timetable service to reduce complexity in handling academic periods.

---

### 2. Feat: Fix Auth Responses & Smart Semester Detection
- **Commit Hash**: `cafccc585a7ce4cf1e38fc5a82e1c3d979f083b7`
- **Affected Files**:
    - `src/main/java/com/app/nonstop/domain/timetable/dto/SemesterDto.java`
    - `src/main/java/com/app/nonstop/domain/timetable/service/TimetableService.java`
    - `src/main/java/com/app/nonstop/global/config/SecurityConfig.java`
    - `src/main/java/com/app/nonstop/global/security/jwt/JwtTokenProvider.java`

#### Detailed Changes:
- **Uzbekistan Semester Logic**:
    - Implemented a "smart" semester detection system tailored for the Uzbekistan academic calendar.
    - Added `isCurrent` field to `SemesterDto` to allow the frontend to easily highlight the active semester.
    - Updated `TimetableService` with automated logic to determine the current semester based on the current date:
        - **Semester 1**: January to July.
        - **Semester 2**: August to December.
- **Security & JWT Fixes**:
    - **401 Unauthorized Response**: Fixed the authentication entry point in `SecurityConfig.java`. API requests (starting with `/api/`) now correctly return a structured JSON response with an `UNAUTHORIZED` error code instead of a redirect.
    - **Robust JWT Parsing**: Enhanced `JwtTokenProvider.java` to gracefully handle `null` values for `universityId` and `isVerified` claims. This is crucial for supporting "Graceful Degradation" where unverified users can still access basic features.

---

## ✅ Test Verification
All tests have been executed successfully after the recent changes.

- **Build Status**: SUCCESSFUL
- **Tests Run**: All available tests passed
- **Duration**: 11 seconds
- **Date**: 2026-01-19

This confirms that the changes to TimetableService, SecurityConfig, JwtTokenProvider, and SemesterDto do not introduce any regressions.

---

## 💡 Developer Notes
- **Timetable Management**: When querying semesters via `getSemesters`, the system now automatically calculates which semester is active for the user's university.
- **Authentication**: Ensure that clients handle the JSON error response for 401 status codes. The format is:
  ```json
  {
    "success": false,
    "error": {
      "code": "UNAUTHORIZED",
      "message": "Authentication required"
    }
  }
  ```
- **Graceful Degradation**: The backend now explicitly supports JWTs with missing university information, allowing users who haven't completed email verification to use non-restricted features like the personal timetable.

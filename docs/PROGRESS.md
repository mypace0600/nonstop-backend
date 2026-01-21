# Development Progress Report

This document records the recent changes and architectural updates in the **Nonstop** backend.

## 📅 Summary of Changes (2026-01-19)
The recent updates focused on stabilizing the Friends module and expanding User discovery:
1.  **Friends Module Mapping**: Fixed critical MyBatis mapping errors in friends list and requests.
2.  **User Discovery**: Implemented user search and unfriend capabilities.
3.  **WebSocket Handshake**: Enhanced WebSocket configuration for better protocol upgrade support.

---

## 🚀 Commit Details (2026-01-19)

### 1. Fix: Friends Module MyBatis Mapping & DTOs
- **Commit Hash**: `297037f86801bd4af0ec5be3...`
- **Affected Files**:
    - `src/main/resources/mybatis/mappers/friend/FriendMapper.xml`
    - `src/main/java/com/app/nonstop/domain/friend/dto/FriendDto.java`

#### Detailed Changes:
- **ResultMap Association**: Implemented `<resultMap>` with `<association>` for `FriendRequestResponseDto` and `FriendResponseDto`. This fixes the `BadSqlGrammarException` where MyBatis attempted to map `userId` (INT8) into `LocalDateTime` fields.
- **DTO Mutability**: Added `@Setter`, `@NoArgsConstructor`, and `@AllArgsConstructor` to `FriendDto` sub-classes to ensure MyBatis can instantiate and populate nested objects correctly.

---

### 2. Feat: User Search & Unfriend Capability
- **Commit Hash**: `7d6736189578ab...`
- **Affected Files**:
    - `src/main/java/com/app/nonstop/domain/user/controller/UserController.java`
    - `src/main/java/com/app/nonstop/domain/friend/controller/FriendController.java`
    - `src/main/java/com/app/nonstop/domain/user/service/UserService.java`

#### Detailed Changes:
- **Search API**: Added `GET /api/v1/users/search?query=...` to allow users to find others by nickname for friend requests.
- **Unfriend API**: Added `DELETE /api/v1/friends/{friendId}` to allow users to terminate existing friend relationships.
- **Service Layer logic**: Implemented necessary business logic and Mapper methods (`searchByNickname`) to support these features.

---

### 3. Fix: WebSocket Protocol Upgrade (HTTP 400)
- **Commit Hash**: `c76f2a9...`
- **Affected Files**:
    - `src/main/java/com/app/nonstop/global/config/WebSocketConfig.java`

#### Detailed Changes:
- **Pure WebSocket Support**: Enabled a direct WebSocket endpoint at `/ws/v1/chat` alongside the existing SockJS endpoint. This resolves the HTTP 400 "was not upgraded to websocket" error seen by pure STOMP clients.
- **Auth Interceptor Alignment**: Ensured the handshake interceptor correctly processes the `token` parameter from the URL query string.

---

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

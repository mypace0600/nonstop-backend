# Admin Module Implementation Plan

## 1. Overview
The Admin Module provides endpoints for administrators to manage the application, focusing on student ID verification, content moderation (reports), and user management.

## 2. Authentication & Authorization
- **Role-Based Access Control (RBAC):** All endpoints under `/api/v1/admin/**` must be restricted to users with the `ADMIN` role.
- **Security Configuration:** Update `SecurityConfig` to enforce these restrictions.

## 3. API Endpoints

### 3.1 Student ID Verification (University/Major Verification)
**Goal:** Review pending student ID verifications.

*   **List Pending Verifications**
    *   `GET /api/v1/admin/verifications?status=PENDING&page=0&size=20`
    *   Response: List of `AdminVerificationDto` (id, userId, userNickname, universityName, majorName, imageUrl, status, submittedAt).

*   **Approve Verification**
    *   `POST /api/v1/admin/verifications/{id}/approve`
    *   Logic:
        1.  Update `Verification` status to `ACCEPTED`.
        2.  Update `User`'s `isVerified` to `true`.
        3.  Send notification to user (optional/future).

*   **Reject Verification**
    *   `POST /api/v1/admin/verifications/{id}/reject`
    *   Body: `{ "reason": "Image unclear" }`
    *   Logic:
        1.  Update `Verification` status to `REJECTED`.
        2.  Store rejection reason.
        3.  Send notification to user (optional/future).

### 3.2 Report Management (Content Moderation)
**Goal:** Handle user reports on posts, comments, or users.

*   **List Reports**
    *   `GET /api/v1/admin/reports?status=PENDING&page=0&size=20`
    *   Response: List of `AdminReportDto` (id, reporterNickname, targetType, targetId, reason, status, createdAt).

*   **Process Report**
    *   `POST /api/v1/admin/reports/{id}/process`
    *   Body: `{ "action": "BLIND" | "REJECT", "memo": "..." }`
    *   Logic:
        1.  If `BLIND`:
            *   Update `Report` status to `RESOLVED`.
            *   If target is POST/COMMENT: Set content to "Blinded by admin" or verify soft delete logic.
            *   If target is USER: Potentially suspend user (optional).
        2.  If `REJECT`:
            *   Update `Report` status to `REJECTED` (false report).

### 3.3 User Management
**Goal:** View and manage users (e.g., blocking bad actors).

*   **List Users**
    *   `GET /api/v1/admin/users?page=0&size=20&search=...`
    *   Response: List of `AdminUserDto`.

*   **Update User Role**
    *   `PATCH /api/v1/admin/users/{id}/role`
    *   Body: `{ "role": "ADMIN" | "USER" }`

*   **Update User Status (Block/Unblock)**
    *   `PATCH /api/v1/admin/users/{id}/status`
    *   Body: `{ "isActive": boolean }`

## 4. Implementation Steps

### Step 1: DTOs & Enums
*   Create `AdminVerificationDto`, `AdminReportDto`, `AdminUserDto`.
*   Ensure `VerificationStatus` and `ReportStatus` enums are available and sufficient.

### Step 2: Mapper & Repository
*   Create `AdminMapper` interface and `AdminMapper.xml`.
*   Implement queries for:
    *   Selecting pending verifications.
    *   Selecting reports.
    *   Selecting users with filters.
    *   Updating statuses.

### Step 3: Service Layer
*   Create `AdminService` interface and `AdminServiceImpl`.
*   Implement business logic for approvals, rejections, and report processing.
*   Ensure transactional integrity.

### Step 4: Controller Layer
*   Create `AdminController`.
*   Define endpoints and map to Service methods.
*   Add `@PreAuthorize("hasRole('ADMIN')")` or rely on SecurityConfig.

### Step 5: Security Configuration
*   Modify `SecurityConfig.java` to restrict `/api/v1/admin/**` to `ADMIN` role.

## 5. Directory Structure
```
src/main/java/com/app/nonstop/domain/admin/
├── controller/
│   └── AdminController.java
├── service/
│   ├── AdminService.java
│   └── AdminServiceImpl.java
├── dto/
│   ├── AdminVerificationDto.java
│   ├── AdminReportDto.java
│   └── AdminUserDto.java
└── mapper/
    └── AdminMapper.java (interface)

src/main/resources/mybatis/mappers/admin/
└── AdminMapper.xml
```

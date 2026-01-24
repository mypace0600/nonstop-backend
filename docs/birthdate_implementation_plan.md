# Birthdate Feature Implementation Plan

## Overview
Implement the missing `birthDate` registration and validation feature for existing users and ensure it's integrated into the user profile and authentication flow.

## 1. Database & Persistence Layer
- [x] **Verify Schema**: `users` table should have `birth_date` column (Confirmed in v2.5.17 migration).
- [x] **UserMapper**:
    - Add `updateBirthDate` method in `UserMapper` interface.
    - Add corresponding SQL in `UserMapper.xml`.

## 2. Data Transfer Objects (DTO)
- [x] **Create `BirthDateUpdateRequestDto`**:
    - Fields: `birthDate` (LocalDate).
    - Validation: Not null.
- [x] **Update `UserResponseDto`**:
    - Add `birthDate` field.
    - Update static factory method `of(User user)`.

## 3. Business Logic (Service Layer)
- [x] **UserService**:
    - Implement `registerBirthDate(Long userId, LocalDate birthDate)`.
    - **Validation**:
        - Check if user is at least 14 years old (`UnderAgeException`).
        - (Optional) Check if birthdate is already set (if it's immutable).

## 4. API Layer (Controller)
- [x] **UserController**:
    - Add endpoint: `POST /api/v1/users/me/birth-date`.
    - Response: `200 OK`.
    - Error: `400 Bad Request` if under 14.

## 5. Authentication Integration
- [x] **AuthService**:
    - Verify that `TokenResponseDto` builders correctly populate `hasBirthDate` based on the user's entity state.

## 6. Verification
- [x] Build Success.
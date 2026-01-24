# Verification System Refactoring Plan

## 1. Background
Currently, the `users` table and `User` entity contain two verification-related fields:
- `is_verified` (Boolean): Indicates **University Student Verification**.
- `email_verified` (Boolean): Indicates **Signup Identity Verification** (Email ownership).

## 2. Problem
The naming `is_verified` is ambiguous and "awkward" because:
1.  It sounds like a generic "User is verified" flag, but it specifically means "University Verified".
2.  It co-exists with `email_verified`, causing confusion about which "verification" is being referred to.
3.  `verification_method` is also tied to University Verification but its name doesn't explicitly state that.

## 3. Objective
Clarify the purpose of each field by renaming them to be explicit.
- `is_verified` -> **`is_university_verified`**
- `verification_method` -> **`university_verification_method`** (Optional, but recommended for consistency)

## 4. Implementation Steps

### 4.1 Database Schema (Migration)
- Create a new migration script (e.g., `V8_rename_verification_columns.sql`).
- Rename columns in `users` table:
  ```sql
  ALTER TABLE users RENAME COLUMN is_verified TO is_university_verified;
  -- Optional: Rename index if necessary
  ```

### 4.2 Java Entity & DTOs
- **Entity**: `User.java`
  - `Boolean isVerified` -> `Boolean isUniversityVerified`
- **DTOs**:
  - `UserResponseDto`: Rename field `isVerified` -> `isUniversityVerified`.
  - `LoginResponseDto` (TokenResponse): Rename field `isVerified` -> `isUniversityVerified` (or keep JSON key if client impact is too high, but better to rename for consistency).
  - `CustomUserDetails`: Rename field.

### 4.3 MyBatis Mappers
- Update `UserMapper.xml`, `AuthMapper.xml`, and others referencing `is_verified`.
- Update `resultMap` or SQL queries to use the new column name.

### 4.4 Service Layer
- Update `AuthService`, `VerificationService`, `UserService`.
- Ensure logic correctly distinguishes between `emailVerified` (Identity) and `isUniversityVerified` (Student Status).

### 4.5 Security & Token
- Update `JwtTokenProvider` to put `isUniversityVerified` into the token payload (claim).
- Update `Access Token Payload` documentation.

## 5. Affected Components
- `src/main/java/com/app/nonstop/domain/user/entity/User.java`
- `src/main/resources/mybatis/mappers/user/UserMapper.xml`
- `src/main/resources/mybatis/mappers/auth/AuthMapper.xml`
- `src/main/java/com/app/nonstop/domain/auth/dto/TokenResponseDto.java`
- `src/main/java/com/app/nonstop/domain/verification/service/VerificationServiceImpl.java`
- `src/main/java/com/app/nonstop/global/security/user/CustomUserDetails.java`
- `src/main/java/com/app/nonstop/global/security/jwt/JwtTokenProvider.java`

## 6. Verification
- Run tests to ensure `emailVerified` still works for Login/Signup.
- Run tests to ensure `isUniversityVerified` works for University Verification flow.

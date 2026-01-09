# Frontend & Backend Compatibility Report

## ✅ Resolved Backend Issues
The following backend changes have been implemented to support the frontend:
1.  **Signup Data:** `SignUpRequestDto` now accepts `universityId` and `majorId`.
2.  **Persistence:** `AuthMapper.xml` updated to save university and major IDs during signup.
3.  **Validation:**
    *   Password: Min length reduced to 6, complexity requirement removed.
    *   Nickname: Max length increased to 20.

---

## ⚠️ Pending Frontend Tasks

The `nonstop-frontend` needs to be updated to match the new backend capabilities. The current hardcoded strings and logic in `SignupScreenV1` are incompatible.

### 1. University Selection Logic
*   **Current:** Uses a hardcoded list of strings (e.g., `'Inha University in Tashkent'`).
*   **Required:** Fetch universities from the backend API.
    *   **Endpoint:** `GET /api/v1/universities`
    *   **Action:** Call this API on screen load (or `initState`).
    *   **UI:** Populate the dropdown with the `name` from the API response, but store the `id` as the selected value.

### 2. Signup API Call
*   **Current:** Collects nickname, email, password, and `_selectedUniversity` (String).
*   **Required:** Update the signup payload to include `universityId`.
    *   **Payload:**
        ```json
        {
          "email": "user@example.com",
          "password": "password123",
          "nickname": "mynickname",
          "universityId": 123  // Send the ID, not the name
          // "majorId": 456    // Optional: If major selection is added
        }
        ```
    *   **AuthApi Interface:** Update `signUp` method to accept `int? universityId` instead of `String? university`.

### 3. Major Selection (Optional but Recommended)
*   **Current:** Missing from `SignupScreenV1`.
*   **Required:** If users should select a major during signup:
    *   **Action:** When a university is selected, fetch its majors.
    *   **Endpoint:** `GET /api/v1/universities/{universityId}/majors`
    *   **UI:** Add a "Select Major" dropdown that enables after university selection.
    *   **Payload:** Include `majorId` in the signup request.

### 4. Validation UI
*   **Current:** Checks for `_selectedUniversity == null`.
*   **Required:** Ensure `_selectedUniversityId != null` before submission. The password and nickname validation logic on the frontend is now compatible with the backend, so no changes are strictly needed there, but ensuring they match the backend's new rules (Password min 6, Nickname max 20) is good practice.

## Implementation Checklist
- [ ] Create/Update `University` model in Flutter to include `id` and `name`.
- [ ] Implement `getUniversities` in `UniversityRepository`.
- [ ] Update `SignupScreenV1` to fetch universities on load.
- [ ] Replace `List<String> _universities` with `List<University> _universities`.
- [ ] Update `DropdownButton` to work with `University` objects or IDs.
- [ ] Update `AuthApi.signUp` to send `universityId`.
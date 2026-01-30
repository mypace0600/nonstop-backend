# Nonstop MVP To-Do List
**Last Updated: 2026-01-30**

## Overview

| Priority | Total | Completed | Remaining |
|----------|-------|-----------|-----------|
| P0 (Critical) | 4 | 0 | 4 |
| P1 (Important) | 4 | 0 | 4 |
| P2 (Nice-to-have) | 3 | 0 | 3 |

---

## P0 - Critical (MVP 출시 필수)

### 1. 비밀번호 재설정 기능
- **Status**: ❌ Not Started
- **PRD Section**: 3.1.9
- **Description**: 임시 비밀번호 발급 및 강제 변경 기능

**Tasks:**
- [ ] `users` 테이블에 `password_must_change` 컬럼 추가
- [ ] `POST /api/v1/auth/password/reset-request` API 구현
- [ ] 임시 비밀번호 생성 로직 (8자리 영문+숫자)
- [ ] 이메일 발송 템플릿 추가
- [ ] Rate Limit 적용 (1분당 1회)
- [ ] 로그인 응답에 `mustChangePassword` 필드 추가
- [ ] 비밀번호 변경 시 `password_must_change = false` 처리

**Acceptance Criteria:**
- 이메일 가입 사용자만 요청 가능
- Google OAuth 사용자 요청 시 적절한 에러 메시지 반환
- 임시 비밀번호로 로그인 후 비밀번호 변경 강제

---

### 2. 인증 코드 시도 횟수 제한
- **Status**: ❌ Not Started
- **PRD Section**: 9.2
- **Description**: 브루트포스 공격 방지를 위한 시도 횟수 제한

**Tasks:**
- [ ] Redis Key 설계: `verification:attempt:{email}` (TTL 5분)
- [ ] 회원가입 이메일 인증 (`/api/v1/auth/email/verify`) 수정
- [ ] 학교 웹메일 인증 (`/api/v1/verification/email/confirm`) 수정
- [ ] 5회 초과 시 인증 코드 무효화 로직
- [ ] 에러 응답 코드 추가: `MAX_ATTEMPTS_EXCEEDED`

**Acceptance Criteria:**
- 5회 실패 시 해당 인증 코드 즉시 무효화
- 새로운 인증 코드 요청 시 시도 횟수 초기화
- 적절한 에러 메시지 반환

---

### 3. Google OAuth 만 14세 체크
- **Status**: ❌ Not Started
- **PRD Section**: 9.1
- **Description**: 법적 요구사항 - 만 14세 미만 가입 차단

**Tasks:**
- [ ] 정책 결정: Option A (선수집) vs Option B (후차단)
- [ ] `POST /api/v1/auth/google` 응답에 `isNewUser` 필드 추가
- [ ] 신규 사용자 플로우 수정
  - Option A: 생년월일 선수집 후 계정 생성
  - Option B: 계정 생성 후 검증, 미성년자 계정 삭제
- [ ] 클라이언트 가이드 문서 작성

**Acceptance Criteria:**
- 만 14세 미만 사용자는 Google OAuth로도 가입 불가
- 명확한 에러 메시지 제공

---

### 4. 익명 게시판 미인증 사용자 쓰기 차단
- **Status**: ❌ Not Started
- **PRD Section**: 9.5
- **Description**: 악용 방지를 위한 익명 게시판 접근 제어

**Tasks:**
- [ ] `PostService.createPost()` 수정
- [ ] 게시판 타입 체크 로직 추가 (ANONYMOUS + !is_verified → 차단)
- [ ] 에러 응답 코드 추가: `VERIFICATION_REQUIRED_FOR_ANONYMOUS`
- [ ] 에러 응답에 `universityRequired: true` 포함

**Acceptance Criteria:**
- 미인증 사용자는 ANONYMOUS 타입 게시판에 글 작성 불가
- 읽기는 허용
- 인증 안내 메시지 반환

---

## P1 - Important (MVP 품질 향상)

### 5. 게시판 관리 API (Admin)
- **Status**: ❌ Not Started
- **PRD Section**: 3.4.3
- **Description**: 관리자용 게시판 CRUD

**Tasks:**
- [ ] `POST /api/v1/communities/{id}/boards` - 게시판 생성
- [ ] `PATCH /api/v1/boards/{id}` - 게시판 수정
- [ ] `DELETE /api/v1/boards/{id}` - 게시판 삭제 (soft delete)
- [ ] Admin 권한 체크 (`ROLE_ADMIN`, `ROLE_MANAGER`)
- [ ] BoardMapper XML 작성

**Acceptance Criteria:**
- ADMIN/MANAGER 권한만 접근 가능
- 게시판 삭제 시 하위 게시글은 유지 (게시판만 비활성화)

---

### 6. 탈퇴 시 콘텐츠 익명화 검증
- **Status**: ⚠️ Needs Verification
- **PRD Section**: 9.3
- **Description**: 탈퇴 사용자의 게시글/댓글/채팅 처리

**Tasks:**
- [ ] 현재 구현 상태 확인
- [ ] 게시글/댓글 조회 시 탈퇴 사용자 닉네임 "알 수 없음" 처리 확인
- [ ] 채팅 내 탈퇴 사용자 처리 확인
- [ ] 그룹 채팅 자동 퇴장 처리 확인
- [ ] 친구/차단 관계 해제 확인
- [ ] 미구현 항목 보완

---

### 7. 신고 처리 결과 알림
- **Status**: ❌ Not Started
- **PRD Section**: 9.7
- **Description**: 신고자/피신고자에게 처리 결과 통보

**Tasks:**
- [ ] NotificationType에 `REPORT_RESULT` 추가
- [ ] AdminService의 신고 처리 로직에 알림 발송 추가
- [ ] 신고자용 알림 메시지 템플릿
- [ ] 피신고자용 알림 메시지 템플릿 (BLIND/DELETE 시)
- [ ] FCM Push 연동

---

### 8. 학생증 인증 PENDING 중복 요청 방지
- **Status**: ⚠️ Needs Verification
- **PRD Section**: 9.6.2
- **Description**: 동시 인증 요청 방지

**Tasks:**
- [ ] 현재 구현 상태 확인
- [ ] PENDING 상태 요청 존재 시 거부 로직 확인/추가
- [ ] 에러 코드: `VERIFICATION_ALREADY_PENDING`
- [ ] 기존 요청 취소 API 필요 여부 검토

---

## P2 - Nice-to-have (향후 개선)

### 9. 조회수 중복 방지
- **Status**: ❌ Not Started
- **PRD Section**: 3.5 (TODO 주석)
- **Description**: 같은 사용자의 중복 조회수 증가 방지

**Tasks:**
- [ ] Redis Set 활용: `post:view:{postId}` → userId 저장
- [ ] TTL 설정 (예: 24시간)
- [ ] PostService.getPostDetail() 수정
- [ ] 또는 Cookie 기반 방식 검토

---

### 10. 닉네임 금칙어 필터
- **Status**: ❌ Not Started
- **PRD Section**: 9.10
- **Description**: 부적절한 닉네임 등록 방지

**Tasks:**
- [ ] 금칙어 목록 관리 방안 결정 (DB vs 파일)
- [ ] 필터링 서비스 구현
- [ ] 회원가입/프로필 수정 시 검증 추가
- [ ] 에러 코드: `INAPPROPRIATE_NICKNAME`

---

### 11. 테스트 코드 작성
- **Status**: ❌ Not Started
- **Description**: 주요 서비스 로직 단위 테스트

**Tasks:**
- [ ] AuthService 테스트
- [ ] UserService 테스트
- [ ] ChatService 테스트
- [ ] PostService 테스트
- [ ] VerificationService 테스트
- [ ] Integration Test 작성

---

## Database Migration Required

```sql
-- P0: 비밀번호 재설정
ALTER TABLE users ADD COLUMN password_must_change BOOLEAN DEFAULT FALSE;

-- (Optional) P1: 신고 처리 알림용
-- NotificationType에 REPORT_RESULT 추가 필요
```

---

## Timeline Suggestion

| Phase | Items |
|-------|-------|
| **Week 1** | P0 #1 (비밀번호 재설정), P0 #2 (인증 코드 보안) |
| **Week 2** | P0 #3 (OAuth 연령 체크), P0 #4 (익명 게시판) |
| **Week 3** | P1 #5 (게시판 관리), P1 #6 (탈퇴 검증) |
| **Week 4** | P1 #7-8, P2 items (시간 허용 시) |

---

## Related Documents

- [PRD Document](./prd_draft.md) - Section 9, 10 참조
- [Chat System Design](./chatting-docs/chat-system.md)
- [README](../README.md)

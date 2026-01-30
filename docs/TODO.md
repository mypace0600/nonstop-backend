# Nonstop MVP To-Do List
**Last Updated: 2026-01-30 (v2)**

## Overview

### Backend Status
| Priority | Total | Completed | Remaining |
|----------|-------|-----------|-----------|
| P0 (Critical) | 4 | 0 | 4 |
| P1 (Important) | 4 | 0 | 4 |
| P2 (Nice-to-have) | 3 | 0 | 3 |

### Frontend Status
| Priority | Total | Completed | Remaining |
|----------|-------|-----------|-----------|
| P0 (Critical) | 4 | 0 | 4 |
| P1 (Important) | 5 | 0 | 5 |
| P2 (Nice-to-have) | 3 | 0 | 3 |

---

# Backend To-Do

## P0 - Critical (MVP 출시 필수)

### BE-1. 비밀번호 재설정 기능 (보안 강화형)
- **Status**: ❌ Not Started
- **PRD Section**: 3.1.9
- **Description**: 인증 코드 확인 후 비밀번호 변경 기능

**Tasks:**
- [ ] `POST /api/v1/auth/password/send-code` API 구현
  - 이메일 유효성 검증 (가입 여부, authProvider 체크)
  - 6자리 인증 코드 생성 및 Redis 저장 (TTL 5분)
  - 이메일 발송
  - Rate Limit 적용 (1분당 1회)
- [ ] `POST /api/v1/auth/password/reset` API 구현
  - 인증 코드 검증
  - 시도 횟수 체크 (5회 초과 시 무효화)
  - 새 비밀번호 유효성 검증
  - 비밀번호 업데이트 (bcrypt 암호화)
  - Redis 인증 코드 삭제
- [ ] Redis Key 설계
  - `password:reset:code:{email}` (TTL 5분)
  - `password:reset:limit:{email}` (TTL 60초)
  - `password:reset:attempt:{email}` (TTL 5분)
- [ ] 이메일 템플릿 추가

**Acceptance Criteria:**
- 이메일 가입 사용자만 요청 가능
- Google OAuth 사용자 요청 시 적절한 에러 메시지 반환
- 인증 코드 확인 전까지 기존 비밀번호 유지 (계정 잠금 방지)
- 5회 시도 초과 시 인증 코드 무효화

---

### BE-2. 인증 코드 시도 횟수 제한
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

### BE-3. Google OAuth 만 14세 체크
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

### BE-4. 익명 게시판 미인증 사용자 쓰기 차단
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

### BE-5. 게시판 관리 API (Admin)
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

### BE-6. 탈퇴 시 콘텐츠 익명화 검증
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

### BE-7. 신고 처리 결과 알림
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

### BE-8. 학생증 인증 PENDING 중복 요청 방지
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

### BE-9. 조회수 중복 방지
- **Status**: ❌ Not Started
- **PRD Section**: 3.5 (TODO 주석)
- **Description**: 같은 사용자의 중복 조회수 증가 방지

**Tasks:**
- [ ] Redis Set 활용: `post:view:{postId}` → userId 저장
- [ ] TTL 설정 (예: 24시간)
- [ ] PostService.getPostDetail() 수정
- [ ] 또는 Cookie 기반 방식 검토

---

### BE-10. 닉네임 금칙어 필터
- **Status**: ❌ Not Started
- **PRD Section**: 9.10
- **Description**: 부적절한 닉네임 등록 방지

**Tasks:**
- [ ] 금칙어 목록 관리 방안 결정 (DB vs 파일)
- [ ] 필터링 서비스 구현
- [ ] 회원가입/프로필 수정 시 검증 추가
- [ ] 에러 코드: `INAPPROPRIATE_NICKNAME`

---

### BE-11. 테스트 코드 작성
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

# Frontend To-Do

## P0 - Critical (MVP 출시 필수)

### FE-1. 비밀번호 재설정 화면
- **Status**: ❌ Not Started
- **PRD Section**: 3.1.9, 11.2.1
- **Description**: 비밀번호 재설정 2단계 화면 구현
- **Dependencies**: BE-1 (백엔드 API 구현 필요)

**Tasks:**
- [ ] `routes.dart`에서 `forgotPassword` 라우트 활성화 (현재 주석 처리됨)
- [ ] **Step 1 화면**: 이메일 입력 → 인증 코드 발송
  - 이메일 입력 필드
  - "인증 코드 발송" 버튼
  - 발송 성공 시 Step 2로 이동
- [ ] **Step 2 화면**: 인증 코드 + 새 비밀번호 입력
  - 인증 코드 입력 필드 (6자리)
  - 새 비밀번호 입력 필드
  - 새 비밀번호 확인 필드
  - "비밀번호 변경" 버튼
  - 인증 코드 재발송 링크 (1분 쿨다운)
  - 남은 시간 표시 (5분 카운트다운)
- [ ] `auth_api.dart`에 메서드 추가
  - `sendPasswordResetCode(String email)`
  - `resetPassword(String email, String code, String newPassword)`
- [ ] API 엔드포인트 수정 (`/api/v1/auth/password/send-code`, `/api/v1/auth/password/reset`)
- [ ] 에러 처리 (Rate Limit, 코드 불일치, 만료 등)

**Acceptance Criteria:**
- 로그인 화면에서 "비밀번호 찾기" 버튼 클릭 시 화면 전환
- 2단계 플로우 완료 후 로그인 화면으로 이동
- 에러 메시지 적절히 표시

---

### FE-2. 생년월일 입력 UI
- **Status**: ❌ Not Started
- **PRD Section**: 3.2.2, 11.2.1
- **Description**: 회원가입 시 생년월일 입력 필드 추가

**Tasks:**
- [ ] `SignupScreen` 수정
  - 생년월일 입력 필드 추가 (DatePicker)
  - 만 14세 미만 검증 로직 (프론트엔드 사전 체크)
- [ ] `SignUpRequestDto` 수정
  - `birthDate` 파라미터 추가
- [ ] `auth_api_impl.dart` `signUp()` 메서드 수정
- [ ] Google OAuth 신규 사용자 플로우
  - `isNewUser` 응답 체크
  - 신규 사용자인 경우 생년월일 입력 화면으로 이동

**Acceptance Criteria:**
- 회원가입 시 생년월일 필수 입력
- 만 14세 미만 선택 시 가입 불가 메시지
- Google OAuth 신규 사용자도 생년월일 입력 필수

---

### FE-3. 게시글/댓글 신고 기능
- **Status**: ❌ Not Started
- **PRD Section**: 3.5, 3.6.3
- **Description**: 게시글/댓글 신고 UI 구현

**Tasks:**
- [ ] `BoardDetailScreen` 더보기 메뉴에 "신고" 옵션 추가
- [ ] 신고 모달 구현
  - 신고 사유 선택 (SPAM, HARASSMENT, INAPPROPRIATE_CONTENT, OTHER)
  - 상세 설명 입력 (선택)
  - "신고하기" 버튼
- [ ] `board_api.dart`에 신고 API 연동
  - `reportPost(int postId, String reason, String? description)`
  - `reportComment(int commentId, String reason, String? description)`
- [ ] 댓글 더보기 메뉴에도 신고 옵션 추가
- [ ] 중복 신고 방지 (이미 신고한 경우 안내)

**Acceptance Criteria:**
- 본인 게시글/댓글은 신고 불가
- 신고 완료 시 확인 메시지
- 신고 사유 필수 선택

---

### FE-4. 대학 인증 화면 (학생증/웹메일)
- **Status**: ❌ Not Started
- **PRD Section**: 3.3
- **Description**: 대학생 인증 프로세스 화면 구현

**Tasks:**
- [ ] 인증 방법 선택 화면
  - 학교 웹메일 인증
  - 학생증 사진 인증
- [ ] **웹메일 인증 화면**
  - 학교 이메일 입력 필드 (@univ.ac.kr 등)
  - 도메인 기반 학교 자동 매칭
  - 인증 코드 입력 화면
- [ ] **학생증 인증 화면**
  - 카메라/갤러리 선택
  - 이미지 업로드 (Multipart)
  - 심사 중 안내 메시지
- [ ] 인증 상태 표시 (PENDING, ACCEPTED, REJECTED)
- [ ] API 연동
  - `POST /api/v1/verification/email/request`
  - `POST /api/v1/verification/email/confirm`
  - `POST /api/v1/verification/student-id`

**Acceptance Criteria:**
- 인증 완료 시 프로필에 인증 뱃지 표시
- 반려 시 사유 표시 및 재신청 가능
- PENDING 상태에서 중복 요청 방지

---

## P1 - Important (MVP 품질 향상)

### FE-5. 프로필 수정 화면
- **Status**: ❌ Not Started
- **PRD Section**: 3.2, 11.2.4
- **Description**: 프로필 편집 기능 구현
- **현재 상태**: ProfileScreen에 "Edit Profile - coming soon!" 표시

**Tasks:**
- [ ] 프로필 수정 화면 생성 (`EditProfileScreen`)
- [ ] 수정 가능 필드
  - 닉네임
  - 프로필 이미지 (카메라/갤러리)
  - 자기소개
  - 대학/전공 (변경 시 재인증 안내)
- [ ] 이미지 업로드 연동 (SAS URL 방식)
- [ ] ProfileScreen에서 수정 버튼 연결
- [ ] 닉네임 중복 체크 연동

**Acceptance Criteria:**
- 변경 사항 저장 후 프로필 화면 반영
- 이미지 업로드 중 로딩 표시
- 유효성 검증 에러 메시지 표시

---

### FE-6. 채팅 실제 API 연동
- **Status**: ❌ Not Started (현재 Mock API)
- **PRD Section**: 3.7, 11.2.3
- **Description**: Mock API에서 실제 WebSocket 연동으로 전환

**Tasks:**
- [ ] `chat_api_impl.dart` 생성 (현재 mock만 있음)
- [ ] REST API 연동
  - `GET /api/v1/chat/rooms` - 채팅방 목록
  - `POST /api/v1/chat/rooms` - 1:1 채팅방 생성
  - `GET /api/v1/chat/rooms/{roomId}/messages` - 메시지 조회
- [ ] STOMP/WebSocket 연동
  - 연결: `/ws/v1/chat?token={accessToken}`
  - 구독: `/sub/chat/room/{roomId}`
  - 발행: `/pub/chat/message`
- [ ] 실시간 메시지 수신 처리
- [ ] 읽음 처리 연동

**Acceptance Criteria:**
- 실시간 메시지 송수신
- 채팅방 목록에서 미읽음 카운트 표시
- 연결 끊김 시 자동 재연결

---

### FE-7. 게시판 검색 기능
- **Status**: ❌ Not Started (UI만 있음)
- **PRD Section**: 3.5
- **Description**: 게시판 검색 기능 구현

**Tasks:**
- [ ] 검색 API 연동 확인/구현
- [ ] BoardScreen 검색 바 기능 연결
- [ ] 검색 결과 화면 구현
- [ ] 검색 필터 (제목, 내용, 작성자)
- [ ] 검색 히스토리 (선택)

**Acceptance Criteria:**
- 검색어 입력 후 결과 표시
- 검색 결과 없을 시 안내 메시지

---

### FE-8. 알림 목록 화면
- **Status**: ❌ Not Started
- **PRD Section**: 3.9, 11.2.4
- **Description**: 알림 목록 및 읽음 처리 화면
- **현재 상태**: ProfileScreen에 "Notifications - coming soon!" 표시

**Tasks:**
- [ ] 알림 목록 화면 생성 (`NotificationsScreen`)
- [ ] 알림 타입별 아이콘/스타일 적용
  - POST_LIKE, COMMENT, COMMENT_LIKE
  - FRIEND_REQUEST, FRIEND_ACCEPTED
  - CHAT_MESSAGE, SYSTEM
- [ ] 알림 클릭 시 해당 화면으로 이동
- [ ] 개별/전체 읽음 처리
- [ ] API 연동
  - `GET /api/v1/notifications`
  - `PATCH /api/v1/notifications/{id}/read`
  - `PATCH /api/v1/notifications/read-all`

**Acceptance Criteria:**
- 미읽음 알림 구분 표시
- 클릭 시 해당 게시글/채팅방으로 이동
- 전체 읽음 처리 기능

---

### FE-9. 채팅방 생성 UI
- **Status**: ❌ Not Started
- **PRD Section**: 3.7.1
- **Description**: 1:1/그룹 채팅방 생성 기능
- **현재 상태**: ChatScreen에 TODO 주석만 있음

**Tasks:**
- [ ] 채팅방 생성 버튼 추가
- [ ] 사용자 선택 화면 (친구 목록 기반)
- [ ] 1:1 채팅방 생성 연동
- [ ] 그룹 채팅방 생성 (선택)
  - 그룹명 입력
  - 여러 사용자 선택

**Acceptance Criteria:**
- 생성 완료 후 해당 채팅방으로 이동
- 기존 1:1 채팅방 존재 시 해당 방으로 이동

---

## P2 - Nice-to-have (향후 개선)

### FE-10. 채팅 이미지 전송
- **Status**: ❌ Not Started
- **PRD Section**: 3.7, 3.10.1
- **Description**: 채팅에서 이미지 전송 기능

**Tasks:**
- [ ] 이미지 선택 UI (카메라/갤러리)
- [ ] SAS URL 발급 연동
- [ ] Azure Blob Storage 직접 업로드
- [ ] 업로드 완료 후 메시지 전송
- [ ] 이미지 메시지 표시 UI

**Acceptance Criteria:**
- 이미지 업로드 중 진행률 표시
- 전송 완료 후 채팅방에 이미지 표시

---

### FE-11. 게시글 이미지 첨부
- **Status**: ⚠️ Partial (API만 있음)
- **PRD Section**: 3.5
- **Description**: 게시글 작성 시 이미지 첨부 기능

**Tasks:**
- [ ] CreatePostScreen에 이미지 선택 버튼 추가
- [ ] 다중 이미지 선택 (최대 5개 등)
- [ ] SAS URL 업로드 연동
- [ ] 이미지 미리보기
- [ ] 게시글 상세에서 이미지 표시

---

### FE-12. 공개 시간표 조회
- **Status**: ⚠️ Partial
- **PRD Section**: 3.8.4
- **Description**: 같은 학교 공개 시간표 조회

**Tasks:**
- [ ] 공개 시간표 목록 화면
- [ ] API 연동: `GET /api/v1/timetables/public`
- [ ] 시간표 상세 조회

---

# Database Migration Required

```sql
-- P0: 비밀번호 재설정 (보안 강화형)
-- DB 스키마 변경 불필요! Redis만 사용

-- (Optional) P1: 신고 처리 알림용
-- NotificationType에 REPORT_RESULT 추가 필요
```

**Redis Keys (비밀번호 재설정):**
| Key | TTL | 용도 |
|-----|-----|------|
| `password:reset:code:{email}` | 5분 | 인증 코드 |
| `password:reset:limit:{email}` | 60초 | Rate Limit |
| `password:reset:attempt:{email}` | 5분 | 시도 횟수 |

---

# Implementation Dependencies

```
BE-1 (비밀번호 재설정 API) → FE-1 (비밀번호 재설정 화면)
BE-3 (OAuth 만 14세 체크) → FE-2 (생년월일 입력 UI)
BE-4 (익명 게시판 제한) → FE-4 (대학 인증 화면)
BE-6 (WebSocket 구현 확인) → FE-6 (채팅 실제 연동)
```

---

# Timeline Suggestion

| Phase | Backend | Frontend |
|-------|---------|----------|
| **Week 1** | BE-1 (비밀번호 재설정), BE-2 (인증 코드 보안) | FE-1 (비밀번호 재설정 화면) |
| **Week 2** | BE-3 (OAuth 연령 체크), BE-4 (익명 게시판) | FE-2 (생년월일 입력), FE-3 (신고 기능) |
| **Week 3** | BE-5 (게시판 관리), BE-6 (탈퇴 검증) | FE-4 (대학 인증), FE-5 (프로필 수정) |
| **Week 4** | BE-7-8, P2 items | FE-6 (채팅 연동), FE-7-9 |

---

# Related Documents

- [PRD Document](./prd_draft.md) - Section 9, 10, 11 참조
- [Chat System Design](./chatting-docs/chat-system.md)
- [Frontend Progress](../../nonstop-frontend/docs/PROGRESS.md)
- [README](../README.md)

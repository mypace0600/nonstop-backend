# 미구현 기능 정리

**기준:** prd_draft.md (Golden Master v2.1)
**최종 업데이트:** 2026-01-15

---

## 📊 전체 현황

| 상태 | 개수 | 비율 |
|------|------|------|
| ✅ 구현 완료 | 72개 API | 97% |
| ❌ 미구현 | 2개 API | 3% |

---

## 🚨 미구현 항목

### 1. 이메일 대학 인증 (핵심 기능)

| 상태 | 엔드포인트 | 설명 |
|------|-----------|------|
| ❌ | `POST /api/v1/verification/email/request` | 인증 코드 발송 |
| ❌ | `POST /api/v1/verification/email/confirm` | 인증 코드 확인 |

**PRD 요구사항 (3.3.1):**

```
1. 인증 요청 (POST /api/v1/verification/email/request)
   - 학교 웹메일 주소 입력 (예: user@univ.ac.kr)
   - 도메인으로 대학 식별 (universities 테이블 매칭)
   - 6자리 난수 코드 생성
   - Redis TTL 5분 설정
   - 메일 발송

2. 코드 확인 (POST /api/v1/verification/email/confirm)
   - 코드 일치 + 시간 검증
   - 시간 만료 시: 400 Bad Request
   - 성공 시: university_id, is_verified=true, verification_method=EMAIL 업데이트
   - 성공 후 코드 즉시 파기 (재사용 방지)
```

**참고:** `docs/uni/university_email_verification_plan.md` 계획 문서 존재

---

## ⚠️ 부분 구현 / 개선 필요 항목

### 1. Rate Limiting (분당 60회 제한)

| 범위 | 상태 | 비고 |
|------|------|------|
| WebSocket 채팅 메시지 | ✅ 구현됨 | Redis 기반, 60/분 제한 |
| **HTTP REST API (POST/PATCH/DELETE)** | ❌ **미구현** | PRD 요구사항 미충족 |

**현재 상태:**
- WebSocket: `WebSocketRateLimitInterceptor.java`에서 Redis 카운터 사용
- HTTP API: Bucket4j 의존성은 있으나 미적용

**필요 작업:**
- POST/PATCH/DELETE 쓰기 API에 분당 60회 제한 적용
- 인터셉터 또는 @Aspect 기반 구현 필요

---

### 2. 학생증 인증 동시성 문제

**상태:** ⚠️ TODO 미해결

**문제점:** `VerificationServiceImpl.java:61-62`
```java
// TODO: (동시성) 이 select와 insert 사이에 여러 요청이 동시에 들어올 경우
// 중복 PENDING 요청이 생성될 수 있습니다.
// student_verification_requests 테이블에 (user_id, status='PENDING')에 대한
// 조건부 Unique 인덱스 추가를 고려해야 합니다.
```

**현재 DB 인덱스:**
```sql
CREATE UNIQUE INDEX ux_student_verification_user
  ON student_verification_requests(user_id);
```
- user_id에만 UNIQUE → status='PENDING' 조건부 인덱스 아님

**필요 작업:**
- PostgreSQL 조건부 인덱스 생성:
  ```sql
  CREATE UNIQUE INDEX ux_student_verification_pending
    ON student_verification_requests(user_id)
    WHERE status = 'PENDING';
  ```
- 또는 서비스 레벨에서 비관적 락 사용

---

## ✅ 구현 완료된 주요 기능

| 도메인 | 상태 | 비고 |
|--------|------|------|
| Authentication (JWT, OAuth) | ✅ | RTR, 자동 로그인 포함 |
| Refresh Token Rotation | ✅ | 재발급 시 기존 토큰 폐기 + 새 토큰 발급 |
| 자동 로그인 | ✅ | refresh API에서 새 토큰 쌍 반환 |
| User Management | ✅ | |
| Community & Boards | ✅ | 공통/학교별 접근 제어 |
| Posts & Comments | ✅ | 계층형 댓글, 좋아요, 신고 |
| Friends & Block | ✅ | |
| Chat (Kafka, WebSocket) | ✅ | 1:1, 그룹 채팅 |
| Timetable | ✅ | 시간 중복 검증, 소유권 검증 |
| Notifications (FCM) | ✅ | |
| File Upload (Azure SAS) | ✅ | |
| 학생증 사진 인증 | ✅ | 동시성 이슈 별도 |

---

## 📋 구현 우선순위

| 순위 | 항목 | 중요도 | 난이도 |
|------|------|--------|--------|
| 1 | 이메일 대학 인증 API | 🔴 높음 | 중간 |
| 2 | HTTP REST API Rate Limiting | 🔴 높음 | 낮음 |
| 3 | 학생증 인증 동시성 해결 | 🟡 중간 | 낮음 |

---

## 📁 관련 문서

- PRD: `docs/prd_draft.md`
- 이메일 인증 계획: `docs/uni/university_email_verification_plan.md`
- 자동 로그인 설계: `docs/auth-auto-login-plan.md`
- 시간표 개발 계획: `docs/timetable_dev_plan.md`

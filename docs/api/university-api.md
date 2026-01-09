# 대학교 선택 API 가이드

**작성일:** 2026-01-10
**브랜치:** feature/university

---

## 개요

회원이 자신의 대학교와 전공을 선택/설정할 수 있는 API입니다.

### 주요 기능
- 대학교 목록 조회 (검색, 지역 필터 지원)
- 대학교별 전공 목록 조회
- 회원 대학교/전공 설정

---

## API 목록

| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|:----:|
| GET | `/api/v1/universities` | 대학교 목록 조회 | - |
| GET | `/api/v1/universities/{universityId}` | 대학교 상세 조회 | - |
| GET | `/api/v1/universities/{universityId}/majors` | 전공 목록 조회 | - |
| GET | `/api/v1/universities/regions` | 지역 목록 조회 | - |
| PATCH | `/api/v1/users/me/university` | 내 대학교/전공 설정 | JWT |

---

## 1. 대학교 목록 조회

대학교 목록을 조회합니다. 검색어와 지역으로 필터링할 수 있습니다.

### Request

```
GET /api/v1/universities
GET /api/v1/universities?keyword=서울
GET /api/v1/universities?region=서울
GET /api/v1/universities?keyword=대&region=서울
```

| Parameter | Type | Required | Description |
|-----------|------|:--------:|-------------|
| keyword | String | X | 대학교 이름 검색어 (부분 일치) |
| region | String | X | 지역 필터 (정확히 일치) |

### Response

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "서울대학교",
      "region": "서울",
      "logoImageUrl": null
    },
    {
      "id": 2,
      "name": "연세대학교",
      "region": "서울",
      "logoImageUrl": null
    }
  ],
  "error": null
}
```

---

## 2. 대학교 상세 조회

특정 대학교의 상세 정보를 조회합니다.

### Request

```
GET /api/v1/universities/1
```

### Response

```json
{
  "success": true,
  "data": {
    "id": 1,
    "name": "서울대학교",
    "region": "서울",
    "logoImageUrl": null
  },
  "error": null
}
```

### Error

```json
// 존재하지 않는 대학교
{
  "success": false,
  "data": null,
  "error": {
    "code": "NOT_FOUND",
    "message": "University not found: 999"
  }
}
```

---

## 3. 전공 목록 조회

특정 대학교의 전공 목록을 조회합니다. 검색어로 필터링할 수 있습니다.

### Request

```
GET /api/v1/universities/1/majors
GET /api/v1/universities/1/majors?keyword=컴퓨터
```

| Parameter | Type | Required | Description |
|-----------|------|:--------:|-------------|
| keyword | String | X | 전공 이름 검색어 (부분 일치) |

### Response

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "name": "컴퓨터공학부"
    },
    {
      "id": 2,
      "name": "전기정보공학부"
    },
    {
      "id": 3,
      "name": "경영학과"
    }
  ],
  "error": null
}
```

---

## 4. 지역 목록 조회

대학교 필터링에 사용할 수 있는 지역 목록을 조회합니다.

### Request

```
GET /api/v1/universities/regions
```

### Response

```json
{
  "success": true,
  "data": [
    "대전",
    "서울",
    "포항"
  ],
  "error": null
}
```

---

## 5. 내 대학교/전공 설정

로그인한 회원의 대학교와 전공을 설정합니다.

### Request

```
PATCH /api/v1/users/me/university
Authorization: Bearer {accessToken}
Content-Type: application/json

{
  "universityId": 1,
  "majorId": 5
}
```

| Field | Type | Required | Description |
|-------|------|:--------:|-------------|
| universityId | Long | O | 대학교 ID |
| majorId | Long | X | 전공 ID (선택사항) |

### Response (성공)

```json
{
  "success": true,
  "data": null,
  "error": null
}
```

### Error Cases

```json
// 1. 인증 실패 (토큰 없음/만료)
HTTP 401
{
  "success": false,
  "data": null,
  "error": {
    "code": "UNAUTHORIZED",
    "message": "Authentication required"
  }
}

// 2. 존재하지 않는 대학교
HTTP 404
{
  "success": false,
  "data": null,
  "error": {
    "code": "NOT_FOUND",
    "message": "University not found: 999"
  }
}

// 3. 전공이 해당 대학교에 속하지 않음
HTTP 400
{
  "success": false,
  "data": null,
  "error": {
    "code": "BAD_REQUEST",
    "message": "Major does not belong to the selected university"
  }
}

// 4. universityId 누락
HTTP 400
{
  "success": false,
  "data": null,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "대학교 ID는 필수입니다"
  }
}
```

---

## 프론트엔드 구현 가이드

### 1. 대학교 선택 UI 흐름

```
1. 대학교 선택 화면 진입
   └─ GET /api/v1/universities/regions → 지역 필터 드롭다운 표시
   └─ GET /api/v1/universities → 전체 대학교 목록 표시

2. 사용자가 검색어 입력 또는 지역 선택
   └─ GET /api/v1/universities?keyword=...&region=... → 필터링된 목록

3. 대학교 선택
   └─ GET /api/v1/universities/{id}/majors → 전공 목록 표시

4. 전공 선택 (선택사항)

5. 저장 버튼 클릭
   └─ PATCH /api/v1/users/me/university
```

### 2. 검색 구현 팁

- **Debounce 적용**: 검색어 입력 시 300ms 정도 debounce 적용 권장
- **빈 결과 처리**: 검색 결과가 없을 경우 안내 메시지 표시

### 3. 전공 선택 시 주의사항

- 전공은 **선택사항**입니다. `majorId`를 보내지 않거나 `null`로 보내도 됩니다.
- 대학교를 변경하면 이전에 선택한 전공이 무효화될 수 있으므로, UI에서 대학교 변경 시 전공 선택을 초기화하는 것을 권장합니다.

---

## 데이터 예시 (테스트용)

현재 DB에 등록된 대학교:

| ID | 이름 | 지역 |
|----|------|------|
| 1 | 서울대학교 | 서울 |
| 2 | 연세대학교 | 서울 |
| 3 | 고려대학교 | 서울 |
| 4 | KAIST | 대전 |
| 5 | POSTECH | 포항 |
| ... | ... | ... |

---

## 관련 기존 API

대학교/전공 정보는 기존 프로필 수정 API에서도 설정 가능합니다:

```
PATCH /api/v1/users/me
{
  "universityId": 1,
  "majorId": 5,
  "nickname": "...",
  ...
}
```

단, 새로 추가된 `/api/v1/users/me/university` API는 **전공 유효성 검증**(해당 대학교에 속하는 전공인지)을 수행합니다.

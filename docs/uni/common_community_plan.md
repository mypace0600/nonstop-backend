# 공통 커뮤니티(Common Community) 접근 허용 구현 계획

## 1. 개요
현재 시스템은 대학 인증(`is_verified=true`)이 완료된 사용자만 커뮤니티에 접근할 수 있으며, 본인이 소속된 대학의 커뮤니티만 이용 가능하도록 제한되어 있습니다.
이를 개선하여 **인증 여부와 상관없이 모든 사용자가 접근 가능한 '공통 커뮤니티'** 개념을 도입합니다.

## 2. 변경 사항

### 2.1. 데이터베이스 스키마 (Schema)
`communities` 테이블의 `university_id` 컬럼 제약조건을 완화하여, 특정 대학에 속하지 않는 커뮤니티를 생성할 수 있도록 합니다.

- **Table**: `communities`
- **Column**: `university_id`
- **Change**: `NOT NULL` -> `NULLABLE`
- **Meaning**: `university_id`가 `NULL`인 레코드는 **공통 커뮤니티**로 간주합니다.

### 2.2. 커뮤니티 조회 로직 (CommunityService)
사용자의 상태(인증 여부, 소속 대학)에 따라 조회되는 커뮤니티 목록을 동적으로 구성합니다.

| 사용자 상태 | 조회 대상 | 로직 |
|---|---|---|
| **미인증 사용자** (`isVerified=false`) | 공통 커뮤니티 | `WHERE university_id IS NULL` |
| **인증 사용자** (`isVerified=true`) | 공통 커뮤니티 + 소속 대학 커뮤니티 | `WHERE university_id IS NULL OR university_id = :userUnivId` |

**수정 대상:** `CommunityServiceImpl.getCommunities`
- 기존: 인증 안 되면 빈 리스트 반환
- 변경: 인증 안 되면 공통 커뮤니티 반환, 인증 되면 공통+대학 커뮤니티 합쳐서 반환

### 2.3. 게시판 접근 제어 (BoardService)
게시판 목록 조회 및 게시글 접근 시 권한 검사 로직을 커뮤니티 타입(공통/대학)에 따라 분기합니다.

**수정 대상:** `BoardServiceImpl.getBoardsByCommunityId` 외 관련 접근 제어 로직

1. **커뮤니티 정보 조회**: 요청한 `communityId`의 `university_id` 확인
2. **권한 검사 분기**:
    - **공통 커뮤니티 (`university_id IS NULL`)**:
        - **Pass**: 사용자의 인증 여부/대학 ID와 무관하게 접근 허용
    - **대학 커뮤니티 (`university_id IS NOT NULL`)**:
        - **Check 1**: 사용자 `isVerified == true`
        - **Check 2**: 사용자 `universityId == community.universityId`
        - 위 조건 불만족 시 `AccessDeniedException` 발생

## 3. 구현 단계

1. **Migration**: `communities` 테이블 스키마 변경 (Null 허용)
2. **Mapper**: `CommunityMapper`에 공통 커뮤니티 조회 및 복합 조회(공통 OR 내 대학) 쿼리 추가
3. **Service**:
    - `CommunityService`: 조회 로직 개선
    - `BoardService`: 접근 제어(Security Check) 로직 개선
4. **Test**:
    - 미인증 유저가 공통 커뮤니티 접근 가능한지 테스트
    - 미인증 유저가 타 대학 커뮤니티 접근 불가한지 테스트
    - 인증 유저가 공통 + 본인 대학 커뮤니티 모두 보이는지 테스트

## 4. API 응답 예시 (CommunityListWrapper)

**미인증 사용자 요청 시:**
```json
{
  "communities": [
    { "id": 1, "name": "자유게시판", "type": "COMMON" },
    { "id": 2, "name": "장터", "type": "COMMON" }
  ],
  "universityRequired": false  // 공통 커뮤니티가 있으므로 이용 가능함
}
```
*Note: `universityRequired` 플래그는 프론트엔드에서 "학교 인증 하러가기" 배너 등을 띄우는 용도로 유지하되, 커뮤니티 목록 자체는 내려줍니다.*

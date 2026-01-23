# Issue: 필수 정책 미동의 시 로그인 제한 구현 (#POLICY-LOGIN-BLOCK)

## 1. 개요
서비스 운영 중 법령 개정이나 정책 변경으로 인해 새로운 필수 약관이 추가될 수 있습니다. 기존 사용자나 가입 시점에 필수 항목을 누락한 사용자가 서비스를 계속 이용하기 위해서는 최신 필수 정책에 대한 동의가 반드시 필요합니다. 이를 위해 로그인 시점에 필수 정책 동의 여부를 검증하는 로직을 추가합니다.

## 2. 요구사항
- **로그인 차단**: 활성화된 정책 중 `is_mandatory = true`인 모든 정책에 대해 사용자의 동의 레코드가 존재하지 않으면 로그인을 차단합니다.
- **정보 제공**: 로그인이 차단될 때, 사용자가 동의해야 하는 필수 정책 목록(ID, 제목, URL)을 응답에 포함하여 프론트엔드에서 즉시 동의 화면을 구성할 수 있게 합니다.
- **대상**: 이메일 로그인 및 Google 소셜 로그인 모두에 적용합니다.

## 3. 상세 설계

### 3.1 예외 처리
- **Exception**: `PolicyAgreementRequiredException`
- **Status**: `403 Forbidden`
- **Body**:
  ```json
  {
    "code": "POLICY_AGREEMENT_REQUIRED",
    "message": "필수 약관 동의가 필요합니다.",
    "requiredPolicies": [
      {
        "id": 1,
        "title": "개인정보 처리방침 v2.0",
        "url": "https://..."
      }
    ]
  }
  ```

### 3.2 로직 흐름 (AuthService.login)
1. 사용자 자격 증명 확인 (Email/Password 또는 Google Token)
2. `PolicyService.getMissingMandatoryPolicies(userId)` 호출
3. 누락된 필수 정책이 존재할 경우 `PolicyAgreementRequiredException` 발생
4. 모두 동의했을 경우에만 JWT 토큰 발행 및 응답

### 3.3 DB 조회 로직 (PolicyMapper)
- 활성화된 필수 정책 목록 조회: `SELECT id FROM policies WHERE is_mandatory = true AND is_active = true`
- 사용자가 동의한 정책 목록과 비교 (Except 연산 또는 In-memory 비교)

## 4. 작업 순서
1. [x] `PolicyAgreementRequiredException` 정의
2. [x] `PolicyService`에 미동의 필수 정책 조회 로직 추가
3. [x] `AuthServiceImpl`의 로그인(일반/구글) 로직에 검증 추가
4. [x] 관련 단위 테스트 작성 (PolicyServiceTest 보강)

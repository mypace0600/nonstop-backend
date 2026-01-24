# 클라이언트 업데이트 가이드 (v2.5.15 ~ v2.5.18)

> **작성일**: 2026-01-24
> **대상**: Flutter 프론트엔드 개발자
> **백엔드 버전**: v2.5.17 (현재), v2.5.18 (이메일 인증 API 분리)

---

## 1. 개요

백엔드에서 v2.5.15 ~ v2.5.18 버전에 걸쳐 다음과 같은 주요 기능이 추가/변경되었습니다:

| 버전 | 변경 내용 |
|------|----------|
| v2.5.15 | 필수 정책 미동의 시 로그인 차단 및 재시도(agreedPolicyIds) 프로세스 구현 |
| v2.5.16 | 로그인 응답에 `hasAgreedAllMandatory` 필드 추가, `GET /api/v1/policies/status` API 추가 |
| v2.5.17 | 생년월일(birthDate) 필수화, 만 14세 미만 가입 제한, `hasBirthDate` 필드 추가, 로그인 이력 관리 |
| v2.5.18 | 이메일 인증 API 분리 (`/email/send-verification`, `/email/verify`) |

---

## 2. 프론트엔드 현재 상태 vs 백엔드 요구사항

### 2.1 TokenResponseDto 필드 불일치 ⚠️ **Critical**

**백엔드 응답 (현재):**
```json
{
  "userId": 123,
  "accessToken": "eyJhbG...",
  "refreshToken": "eyJhbG...",
  "emailVerified": true,
  "hasAgreedAllMandatory": true,
  "hasBirthDate": true
}
```

**프론트엔드 현재 (`auth_response_dto.dart`):**
```dart
@freezed
class TokenResponseDto with _$TokenResponseDto {
  const factory TokenResponseDto({
    required String accessToken,
    required String refreshToken,
  }) = _TokenResponseDto;
}
```

**🔧 수정 필요:**
```dart
@freezed
class TokenResponseDto with _$TokenResponseDto {
  const factory TokenResponseDto({
    required int userId,
    required String accessToken,
    required String refreshToken,
    @Default(false) bool emailVerified,
    @Default(false) bool hasAgreedAllMandatory,
    @Default(false) bool hasBirthDate,
  }) = _TokenResponseDto;
}
```

---

### 2.2 SignUpRequestDto 필드 불일치 ⚠️ **Critical**

**백엔드 요구사항 (`SignUpRequestDto.java`):**
```java
@NotBlank private String email;
@NotBlank private String password;
@NotBlank private String nickname;
@NotNull private LocalDate birthDate;  // 필수!
private Long universityId;              // 선택
private Long majorId;                   // 선택
private List<Long> agreedPolicyIds;     // 정책 동의 ID 목록
```

**프론트엔드 현재 (`auth_request_dto.dart`):**
```dart
@freezed
class SignUpRequestDto with _$SignUpRequestDto {
  const factory SignUpRequestDto({
    required String email,
    required String password,
    required String nickname,
    int? universityId,
    int? majorId,
    // birthDate 누락!
    // agreedPolicyIds 누락!
  }) = _SignUpRequestDto;
}
```

**🔧 수정 필요:**
```dart
@freezed
class SignUpRequestDto with _$SignUpRequestDto {
  const factory SignUpRequestDto({
    required String email,
    required String password,
    required String nickname,
    required String birthDate,  // 형식: "2000-01-01" (ISO 8601)
    int? universityId,
    int? majorId,
    List<int>? agreedPolicyIds,  // 정책 동의 ID 목록
  }) = _SignUpRequestDto;
}
```

---

### 2.3 회원가입 응답 변경 (v2.5.18)

**이전 (v2.5.17 이하):**
```json
{ "message": "인증 메일이 발송되었습니다." }
```

**변경 후 (v2.5.18):**
```json
{ "userId": 123, "email": "user@example.com" }
```

**🔧 수정 필요:**
- 회원가입 응답을 위한 새로운 DTO 생성

```dart
@freezed
class SignUpResponseDto with _$SignUpResponseDto {
  const factory SignUpResponseDto({
    required int userId,
    required String email,
  }) = _SignUpResponseDto;

  factory SignUpResponseDto.fromJson(Map<String, dynamic> json) =>
      _$SignUpResponseDtoFromJson(json);
}
```

---

## 3. 신규 API 구현 필요

### 3.1 이메일 인증 API (v2.5.18)

#### 3.1.1 인증 코드 발송 요청

**Endpoint:** `POST /api/v1/auth/email/send-verification`

**Request:**
```json
{ "email": "user@example.com" }
```

**Response (성공):**
```json
{ "success": true, "data": null, "message": "인증 메일이 발송되었습니다." }
```

**에러 케이스:**
| Status | Code | Message |
|--------|------|---------|
| 404 | USER_NOT_FOUND | 해당 이메일의 사용자를 찾을 수 없습니다 |
| 400 | ALREADY_VERIFIED | 이미 인증된 이메일입니다 |
| 429 | RATE_LIMIT_EXCEEDED | 1분 후에 다시 시도해주세요 |

**🔧 구현 필요 (auth_api.dart):**
```dart
Future<void> sendEmailVerification(String email);
```

**🔧 구현 필요 (auth_api_impl.dart):**
```dart
@override
Future<void> sendEmailVerification(String email) async {
  try {
    final response = await _dioClient.post(
      '/api/v1/auth/email/send-verification',
      data: {'email': email},
    );

    final apiResponse = response.data as Map<String, dynamic>;
    if (apiResponse['success'] != true) {
      throw ServerException(
        message: apiResponse['message'] ?? '인증 메일 발송에 실패했습니다.',
        statusCode: response.statusCode ?? 500,
      );
    }
  } on DioException catch (e) {
    throw _handleDioError(e);
  }
}
```

#### 3.1.2 인증 코드 확인

**Endpoint:** `POST /api/v1/auth/email/verify`

**Request:**
```json
{ "email": "user@example.com", "code": "123456" }
```

**Response (성공):**
```json
{
  "success": true,
  "data": {
    "userId": 123,
    "accessToken": "eyJhbG...",
    "refreshToken": "eyJhbG...",
    "emailVerified": true,
    "hasAgreedAllMandatory": true,
    "hasBirthDate": true
  }
}
```

**에러 케이스:**
| Status | Code | Message |
|--------|------|---------|
| 400 | VERIFICATION_CODE_MISMATCH | 인증 코드가 일치하지 않습니다 |
| 400 | VERIFICATION_CODE_EXPIRED | 인증 코드가 만료되었습니다 |

**🔧 구현 필요:**
```dart
@override
Future<User> verifyEmail({required String email, required String code}) async {
  try {
    final response = await _dioClient.post(
      '/api/v1/auth/email/verify',
      data: {'email': email, 'code': code},
    );

    final apiResponse = response.data as Map<String, dynamic>;
    if (apiResponse['success'] == true) {
      final tokenData = TokenResponseDto.fromJson(apiResponse['data']);

      await _secureStorageService.saveAccessToken(tokenData.accessToken);
      await _secureStorageService.saveRefreshToken(tokenData.refreshToken);

      return await _fetchAndEmitUserInfo();
    } else {
      throw ServerException(
        message: apiResponse['message'] ?? '인증에 실패했습니다.',
        statusCode: response.statusCode ?? 500,
      );
    }
  } on DioException catch (e) {
    throw _handleDioError(e);
  }
}
```

---

### 3.2 정책 동의 API

#### 3.2.1 정책 목록 조회

**Endpoint:** `GET /api/v1/policies`

**Response:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "type": "TERMS_OF_SERVICE",
      "title": "서비스 이용약관",
      "url": "https://cdn.nonstop.app/policies/terms.html",
      "isMandatory": true,
      "version": "1.0"
    },
    {
      "id": 2,
      "type": "PRIVACY_POLICY",
      "title": "개인정보 처리방침",
      "url": "https://cdn.nonstop.app/policies/privacy.html",
      "isMandatory": true,
      "version": "1.0"
    },
    {
      "id": 3,
      "type": "MARKETING",
      "title": "마케팅 정보 수신 동의",
      "url": null,
      "isMandatory": false,
      "version": "1.0"
    }
  ]
}
```

**🔧 신규 DTO 생성 필요:**
```dart
@freezed
class PolicyDto with _$PolicyDto {
  const factory PolicyDto({
    required int id,
    required String type,
    required String title,
    String? url,
    required bool isMandatory,
    required String version,
  }) = _PolicyDto;

  factory PolicyDto.fromJson(Map<String, dynamic> json) =>
      _$PolicyDtoFromJson(json);
}
```

#### 3.2.2 정책 동의 상태 조회

**Endpoint:** `GET /api/v1/policies/status`

**Response:**
```json
{
  "success": true,
  "data": {
    "hasAgreedAllMandatory": false,
    "unagreedPolicies": [
      { "id": 1, "title": "서비스 이용약관", "isMandatory": true }
    ]
  }
}
```

#### 3.2.3 정책 동의 처리

**Endpoint:** `POST /api/v1/policies/agree`

**Request:**
```json
{ "policyIds": [1, 2, 3] }
```

**Response:**
```json
{ "success": true, "data": null, "message": "정책 동의가 완료되었습니다." }
```

---

### 3.3 생년월일 등록 API (기존 사용자용)

**Endpoint:** `POST /api/v1/users/me/birth-date`

**Request:**
```json
{ "birthDate": "2000-01-01" }
```

**Response (성공):**
```json
{ "success": true, "data": null }
```

**에러 케이스:**
| Status | Code | Message |
|--------|------|---------|
| 400 | UNDER_AGE_LIMIT | 만 14세 미만은 서비스 이용이 제한됩니다 |
| 400 | BIRTH_DATE_ALREADY_SET | 이미 생년월일이 등록되어 있습니다 |

---

## 4. 회원가입 플로우 변경

### 4.1 기존 플로우 (프론트엔드 현재)

```
1. 이메일/비밀번호/닉네임 입력
2. 대학교 선택
3. UI에서 정책 동의 체크 (API 미연동)
4. POST /api/v1/auth/signup
5. 성공 시 바로 로그인 시도
6. 홈 화면 진입
```

### 4.2 새로운 플로우 (v2.5.18)

```
1. 이메일/비밀번호/닉네임 입력
2. 생년월일 입력 (필수, 만 14세 이상 검증)
3. 대학교 선택 (선택)
4. 정책 목록 조회 (GET /api/v1/policies)
5. 정책 동의 UI 표시 및 동의 체크
6. POST /api/v1/auth/signup
   - birthDate 포함
   - agreedPolicyIds 포함
7. 성공 시 이메일 인증 화면으로 이동
8. POST /api/v1/auth/email/send-verification
9. 사용자 이메일 확인 후 인증 코드 입력
10. POST /api/v1/auth/email/verify
11. 성공 시 토큰 저장 및 홈 화면 진입
```

---

## 5. 로그인 플로우 변경

### 5.1 기존 플로우

```
1. 이메일/비밀번호 입력
2. POST /api/v1/auth/login
3. 토큰 저장
4. 홈 화면 진입
```

### 5.2 새로운 플로우 (v2.5.16+)

```
1. 이메일/비밀번호 입력
2. POST /api/v1/auth/login
3. 응답 확인:
   a. emailVerified == false → 이메일 인증 화면
   b. hasAgreedAllMandatory == false → 정책 동의 화면
   c. hasBirthDate == false → 생년월일 입력 화면
   d. 모두 true → 홈 화면 진입
4. 토큰 저장
```

**🔧 auth_provider.dart 수정 필요:**
```dart
Future<AuthFlowResult> signIn(String email, String password) async {
  state = state.copyWith(isLoading: true, failure: null);

  final result = await _signInUseCase(SignInParams(email: email, password: password));

  return result.fold(
    (failure) {
      state = state.copyWith(isLoading: false, failure: failure);
      return AuthFlowResult.error;
    },
    (authResult) {
      state = state.copyWith(isLoading: false, user: authResult.user);

      // 상태에 따른 화면 분기
      if (!authResult.emailVerified) {
        return AuthFlowResult.needsEmailVerification;
      }
      if (!authResult.hasAgreedAllMandatory) {
        return AuthFlowResult.needsPolicyAgreement;
      }
      if (!authResult.hasBirthDate) {
        return AuthFlowResult.needsBirthDate;
      }
      return AuthFlowResult.success;
    },
  );
}

enum AuthFlowResult {
  success,
  needsEmailVerification,
  needsPolicyAgreement,
  needsBirthDate,
  error,
}
```

---

## 6. 403 에러 처리 (PolicyAgreementFilter)

백엔드 `PolicyAgreementFilter`가 활성화되어 있어, **필수 정책 미동의 사용자**가 일반 API를 호출하면 `403 Forbidden`이 반환됩니다.

**에러 응답 형식:**
```json
{
  "success": false,
  "data": {
    "requiredPolicies": [
      { "id": 1, "title": "서비스 이용약관", "isMandatory": true }
    ]
  },
  "message": "필수 정책에 동의해야 합니다."
}
```

**🔧 DioClient 인터셉터에 처리 로직 추가:**
```dart
// _ErrorInterceptor 또는 _AuthInterceptor에 추가
if (response.statusCode == 403) {
  final data = response.data as Map<String, dynamic>?;
  if (data?['message']?.contains('정책') == true) {
    // 정책 동의 화면으로 리다이렉트
    _redirectToPolicyAgreement();
    return;
  }
}
```

---

## 7. UI 수정 사항

### 7.1 회원가입 화면 (`signup_screen_v1.dart`)

1. **생년월일 입력 필드 추가**
   - DatePicker 사용
   - 만 14세 이상만 가입 가능 (클라이언트 사전 검증)

2. **정책 동의 UI 개선**
   - 현재: 로컬 상태로만 관리
   - 변경: `GET /api/v1/policies`로 정책 목록 조회 후 동적 렌더링
   - 동의한 정책 ID 목록을 회원가입 요청에 포함

3. **이메일 인증 화면 추가**
   - 회원가입 성공 후 이동
   - 6자리 인증 코드 입력 필드
   - 재발송 버튼 (1분 쿨다운)

### 7.2 로그인 화면 (`login_screen_v1.dart`)

1. **로그인 성공 후 상태 체크**
   - `emailVerified`, `hasAgreedAllMandatory`, `hasBirthDate` 확인
   - 각 상태에 따라 적절한 화면으로 분기

### 7.3 신규 화면 생성 필요

| 화면 | 용도 |
|------|------|
| `email_verification_screen.dart` | 이메일 인증 코드 입력 |
| `policy_agreement_screen.dart` | 정책 동의 (로그인 후) |
| `birth_date_screen.dart` | 생년월일 입력 (기존 사용자) |

---

## 8. 체크리스트

### 8.1 DTO 수정

- [ ] `TokenResponseDto` - 6개 필드로 확장
- [ ] `SignUpRequestDto` - `birthDate`, `agreedPolicyIds` 추가
- [ ] `SignUpResponseDto` - 신규 생성
- [ ] `PolicyDto` - 신규 생성
- [ ] `EmailVerificationRequestDto` - 신규 생성

### 8.2 API 구현

- [ ] `sendEmailVerification(String email)` - 이메일 인증 요청
- [ ] `verifyEmail(String email, String code)` - 인증 코드 확인
- [ ] `getPolicies()` - 정책 목록 조회
- [ ] `getPolicyStatus()` - 정책 동의 상태 조회
- [ ] `agreePolicies(List<int> policyIds)` - 정책 동의
- [ ] `registerBirthDate(String birthDate)` - 생년월일 등록

### 8.3 상태 관리

- [ ] `AuthFlowResult` enum 추가
- [ ] 로그인 성공 후 상태 분기 로직
- [ ] 403 에러 핸들링 (정책 미동의)

### 8.4 UI

- [ ] 회원가입 화면 - 생년월일 필드 추가
- [ ] 회원가입 화면 - 정책 API 연동
- [ ] 이메일 인증 화면 신규 생성
- [ ] 정책 동의 화면 신규 생성
- [ ] 생년월일 입력 화면 신규 생성
- [ ] 로그인 후 화면 분기 로직

---

## 9. 참고 파일

### 백엔드 소스 코드

| 파일 | 설명 |
|------|------|
| `AuthController.java` | 인증 API 엔드포인트 |
| `TokenResponseDto.java` | 로그인 응답 DTO |
| `SignUpRequestDto.java` | 회원가입 요청 DTO |
| `PolicyController.java` | 정책 API 엔드포인트 |

### 백엔드 문서

| 문서 | 위치 |
|------|------|
| PRD 문서 | `docs/prd_draft.md` |
| 이메일 인증 리팩토링 계획 | `docs/v2.5.18-email-verification-refactor.md` |
| Auth 구현 계획 | `docs/auth_implementation_plan_v2.5.17.md` |

---

## 10. 예상 작업량

| 항목 | 예상 규모 |
|------|----------|
| DTO 수정/생성 | 5개 파일 |
| API 구현 | 6개 메서드 |
| 상태 관리 로직 | auth_provider.dart 주요 수정 |
| 신규 화면 | 3개 |
| 기존 화면 수정 | 2개 (signup, login) |

---

## 부록: 에러 코드 정리

| Code | HTTP Status | Message |
|------|-------------|---------|
| `UNDER_AGE_LIMIT` | 400 | 만 14세 미만은 서비스 이용이 제한됩니다 |
| `USER_NOT_FOUND` | 404 | 해당 이메일의 사용자를 찾을 수 없습니다 |
| `ALREADY_VERIFIED` | 400 | 이미 인증된 이메일입니다 |
| `RATE_LIMIT_EXCEEDED` | 429 | 1분 후에 다시 시도해주세요 |
| `VERIFICATION_CODE_MISMATCH` | 400 | 인증 코드가 일치하지 않습니다 |
| `VERIFICATION_CODE_EXPIRED` | 400 | 인증 코드가 만료되었습니다 |
| `POLICY_AGREEMENT_REQUIRED` | 403 | 필수 정책에 동의해야 합니다 |
| `BIRTH_DATE_ALREADY_SET` | 400 | 이미 생년월일이 등록되어 있습니다 |

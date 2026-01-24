package com.app.nonstop.domain.user.controller;

import com.app.nonstop.domain.university.dto.UserUniversityRequestDto;
import com.app.nonstop.domain.user.dto.BirthDateUpdateRequestDto;
import com.app.nonstop.domain.user.dto.PasswordUpdateRequestDto;
import com.app.nonstop.domain.user.dto.ProfileUpdateRequestDto;
import com.app.nonstop.domain.user.dto.UserResponseDto;
import com.app.nonstop.domain.user.dto.VerificationStatusResponseDto;
import com.app.nonstop.domain.user.service.UserService;
import com.app.nonstop.global.common.response.ApiResponse;
import com.app.nonstop.global.security.user.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 사용자 관련 API 요청을 처리하는 컨트롤러입니다.
 * 내 정보 조회, 프로필 수정, 비밀번호 변경, 회원 탈퇴, 인증 상태 조회 등 사용자 본인과 관련된 작업을 담당합니다.
 */
@Tag(name = "User", description = "사용자 정보 관련 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 현재 로그인된 사용자의 상세 정보를 조회합니다.
     *
     * @param customUserDetails Spring Security에서 주입한 현재 인증된 사용자 정보
     * @return ApiResponse<UserResponseDto> 사용자 상세 정보 응답
     */
    @Operation(summary = "내 정보 조회", description = "현재 로그인된 사용자의 상세 정보를 조회합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponseDto>> getMyInfo(
            @Parameter(hidden = true) // Swagger 문서에서 파라미터 입력을 숨기기 위한 어노테이션
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        UserResponseDto myInfo = userService.getMyInfo(customUserDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(myInfo));
    }

    /**
     * 현재 로그인된 사용자의 프로필 정보(닉네임, 학교, 전공, 프로필 이미지, 자기소개, 선호 언어)를 수정합니다.
     *
     * @param customUserDetails Spring Security에서 주입한 현재 인증된 사용자 정보
     * @param requestDto 업데이트할 프로필 정보 DTO
     * @return ApiResponse<?> 성공 응답 (데이터 없음)
     */
    @Operation(summary = "내 정보 수정", description = "현재 로그인된 사용자의 프로필 정보(닉네임, 학교, 전공 등)를 수정합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<?>> updateMyProfile(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody @Valid ProfileUpdateRequestDto requestDto
    ) {
        userService.updateProfile(customUserDetails.getUserId(), requestDto);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 현재 로그인된 사용자의 비밀번호를 변경합니다.
     * 이메일로 가입한 사용자만 비밀번호 변경이 가능합니다.
     *
     * @param customUserDetails Spring Security에서 주입한 현재 인증된 사용자 정보
     * @param requestDto 현재 비밀번호와 새 비밀번호 정보 DTO
     * @return ApiResponse<?> 성공 응답 (데이터 없음)
     */
    @Operation(summary = "비밀번호 변경", description = "현재 로그인된 사용자의 비밀번호를 변경합니다. 이메일 가입자만 가능합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<?>> updateMyPassword(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody @Valid PasswordUpdateRequestDto requestDto
    ) {
        userService.updatePassword(customUserDetails.getUserId(), requestDto);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 현재 로그인된 사용자의 계정을 비활성화(soft delete)합니다.
     * - 실제 데이터 삭제가 아닌 `deleted_at` 필드를 업데이트하는 방식입니다.
     *
     * @param customUserDetails Spring Security에서 주입한 현재 인증된 사용자 정보
     * @return ApiResponse<?> 성공 응답 (데이터 없음)
     */
    @Operation(summary = "회원 탈퇴", description = "현재 로그인된 사용자의 계정을 비활성화(soft delete)합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @DeleteMapping("/me")
    public ResponseEntity<ApiResponse<?>> deactivateMyAccount(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        userService.deactivateAccount(customUserDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 현재 로그인된 사용자의 대학생 인증 상태 및 인증 방식을 조회합니다.
     *
     * @param customUserDetails Spring Security에서 주입한 현재 인증된 사용자 정보
     * @return ApiResponse<VerificationStatusResponseDto> 인증 상태 응답
     */
    @Operation(summary = "인증 상태 조회", description = "현재 로그인된 사용자의 대학생 인증 상태 및 인증 방식을 조회합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me/verification-status")
    public ResponseEntity<ApiResponse<VerificationStatusResponseDto>> getMyVerificationStatus(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails customUserDetails
    ) {
        VerificationStatusResponseDto responseDto = userService.getVerificationStatus(customUserDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(responseDto));
    }

    /**
     * 현재 로그인된 사용자의 대학교 및 전공 정보를 설정합니다.
     *
     * @param customUserDetails Spring Security에서 주입한 현재 인증된 사용자 정보
     * @param requestDto 대학교 ID와 전공 ID 정보 DTO
     * @return ApiResponse<?> 성공 응답 (데이터 없음)
     */
    @Operation(summary = "대학교/전공 설정", description = "현재 로그인된 사용자의 대학교 및 전공 정보를 설정합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @PatchMapping("/me/university")
    public ResponseEntity<ApiResponse<?>> updateMyUniversity(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody @Valid UserUniversityRequestDto requestDto
    ) {
        userService.updateUniversity(
                customUserDetails.getUserId(),
                requestDto.getUniversityId(),
                requestDto.getMajorId()
        );
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 사용자를 검색합니다. (친구 추가용)
     *
     * @param query 검색어 (닉네임)
     * @return 검색된 사용자 목록
     */
    @Operation(summary = "사용자 검색", description = "닉네임으로 사용자를 검색합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<UserResponseDto>>> searchUsers(
            @RequestParam String query
    ) {
        List<UserResponseDto> searchResults = userService.searchUsers(query);
        return ResponseEntity.ok(ApiResponse.success(searchResults));
    }

    @Operation(summary = "생년월일 등록", description = "만 14세 이상만 등록 가능합니다. 최초 1회만 가능합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/me/birth-date")
    public ResponseEntity<ApiResponse<?>> registerBirthDate(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody @Valid BirthDateUpdateRequestDto requestDto
    ) {
        userService.registerBirthDate(customUserDetails.getUserId(), requestDto.getBirthDate());
        return ResponseEntity.ok(ApiResponse.success());
    }
}
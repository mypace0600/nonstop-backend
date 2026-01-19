package com.app.nonstop.domain.user.service;

import com.app.nonstop.domain.user.dto.UserResponseDto;
import com.app.nonstop.domain.user.dto.VerificationStatusResponseDto;

import java.util.List;

public interface UserService {

    /**
     * 현재 로그인된 사용자의 정보를 조회합니다.
     *
     * @param userId 현재 로그인된 사용자의 ID
     * @return 사용자 정보 DTO
     */
    UserResponseDto getMyInfo(Long userId);

    /**
     * 사용자 프로필 정보를 수정합니다.
     *
     * @param userId     수정할 사용자의 ID
     * @param requestDto 수정할 프로필 정보
     */
    void updateProfile(Long userId, com.app.nonstop.domain.user.dto.ProfileUpdateRequestDto requestDto);

    /**
     * 사용자 비밀번호를 변경합니다.
     *
     * @param userId     변경할 사용자의 ID
     * @param requestDto 현재 비밀번호와 새 비밀번호 정보
     */
    void updatePassword(Long userId, com.app.nonstop.domain.user.dto.PasswordUpdateRequestDto requestDto);

    /**
     * 회원 계정을 비활성화(soft delete)합니다.
     *
     * @param userId 탈퇴할 사용자의 ID
     */
    void deactivateAccount(Long userId);

    /**
     * 현재 로그인된 사용자의 대학생 인증 상태 및 인증 방식을 조회합니다.
     *
     * @param userId 현재 로그인된 사용자의 ID
     * @return 인증 상태 응답 DTO
     */
    VerificationStatusResponseDto getVerificationStatus(Long userId);

    /**
     * 사용자의 대학교 및 전공 정보를 설정합니다.
     *
     * @param userId       사용자 ID
     * @param universityId 대학교 ID
     * @param majorId      전공 ID (선택)
     */
    void updateUniversity(Long userId, Long universityId, Long majorId);

    /**
     * 닉네임으로 사용자를 검색합니다.
     *
     * @param query 검색어
     * @return 검색된 사용자 목록
     */
    List<UserResponseDto> searchUsers(String query);
}

package com.app.nonstop.domain.user.service;

import com.app.nonstop.domain.user.dto.UserResponseDto;

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
}

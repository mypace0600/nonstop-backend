package com.app.nonstop.domain.device.service;

import com.app.nonstop.domain.device.dto.DeviceTokenRequestDto;

import java.util.List;

public interface DeviceService {

    /**
     * FCM 디바이스 토큰을 등록하거나 갱신합니다. (Upsert)
     *
     * @param userId     요청한 사용자의 ID
     * @param requestDto 디바이스 토큰 정보
     */
    void registerOrUpdateDeviceToken(Long userId, DeviceTokenRequestDto requestDto);

    /**
     * 특정 사용자의 활성화된 모든 디바이스 토큰을 조회합니다.
     *
     * @param userId 조회할 사용자의 ID
     * @return 활성화된 FCM 토큰 리스트
     */
    List<String> getDeviceTokens(Long userId);
}

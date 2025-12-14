package com.app.nonstop.domain.device.service;

import com.app.nonstop.domain.device.dto.DeviceTokenRequestDto;

public interface DeviceService {

    /**
     * FCM 디바이스 토큰을 등록하거나 갱신합니다. (Upsert)
     *
     * @param userId     요청한 사용자의 ID
     * @param requestDto 디바이스 토큰 정보
     */
    void registerOrUpdateDeviceToken(Long userId, DeviceTokenRequestDto requestDto);
}

package com.app.nonstop.domain.device.service;

import com.app.nonstop.domain.device.dto.DeviceTokenRequestDto;
import com.app.nonstop.domain.device.entity.DeviceToken;
import com.app.nonstop.domain.user.entity.User;
import com.app.nonstop.domain.user.exception.UserNotFoundException;
import com.app.nonstop.mapper.DeviceMapper;
import com.app.nonstop.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 디바이스 토큰 관련 비즈니스 로직을 처리하는 서비스입니다.
 * FCM 디바이스 토큰의 등록 및 갱신(Upsert) 기능을 제공합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class DeviceServiceImpl implements DeviceService {

    private final DeviceMapper deviceMapper;
    private final UserMapper userMapper;

    /**
     * FCM 디바이스 토큰을 등록하거나 갱신합니다. (Upsert 로직)
     * - 동일한 토큰이 이미 존재하면 사용자 정보를 갱신하고 활성 상태로 변경합니다.
     * - 존재하지 않으면 새로운 디바이스 토큰 레코드를 생성합니다.
     *
     * @param userId     요청한 사용자의 ID
     * @param requestDto 디바이스 타입과 FCM 토큰 정보를 포함하는 DTO
     * @throws UserNotFoundException 사용자를 찾을 수 없을 때
     */
    @Override
    public void registerOrUpdateDeviceToken(Long userId, DeviceTokenRequestDto requestDto) {
        // 1. 요청한 사용자의 정보 조회 (사용자가 없으면 UserNotFoundException 발생)
        User user = userMapper.findById(userId).orElseThrow(UserNotFoundException::new);

        // 2. 해당 FCM 토큰이 DB에 이미 존재하는지 조회
        Optional<DeviceToken> optionalDeviceToken = deviceMapper.findByToken(requestDto.getToken());

        if (optionalDeviceToken.isPresent()) {
            // 3-1. 토큰이 이미 존재할 경우: 사용자 정보와 활성 상태를 갱신
            DeviceToken existingToken = optionalDeviceToken.get();
            existingToken.setUser(user); // 토큰이 다른 사용자에게 할당되었을 가능성도 있으므로 사용자 ID 갱신
            existingToken.setIsActive(true); // 비활성화 상태였다면 활성화
            existingToken.setDeviceType(requestDto.getDeviceType()); // 디바이스 타입 갱신
            deviceMapper.update(existingToken);
        } else {
            // 3-2. 토큰이 존재하지 않을 경우: 새로 생성하여 저장
            DeviceToken newDeviceToken = DeviceToken.builder()
                    .user(user)
                    .deviceType(requestDto.getDeviceType())
                    .token(requestDto.getToken())
                    .isActive(true) // 새로 등록 시 활성화
                    .build();
            deviceMapper.save(newDeviceToken);
        }
    }
}

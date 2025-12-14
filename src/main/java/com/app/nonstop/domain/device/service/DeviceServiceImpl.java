package com.app.nonstop.domain.device.service;

import com.app.nonstop.domain.device.dto.DeviceTokenRequestDto;
import com.app.nonstop.domain.device.entity.DeviceToken;
import com.app.nonstop.domain.device.mapper.DeviceMapper;
import com.app.nonstop.domain.user.entity.User;
import com.app.nonstop.domain.user.exception.UserNotFoundException;
import com.app.nonstop.domain.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class DeviceServiceImpl implements DeviceService {

    private final DeviceMapper deviceMapper;
    private final UserMapper userMapper;

    @Override
    public void registerOrUpdateDeviceToken(Long userId, DeviceTokenRequestDto requestDto) {
        Optional<DeviceToken> optionalDeviceToken = deviceMapper.findByToken(requestDto.getToken());
        User user = userMapper.findById(userId).orElseThrow(UserNotFoundException::new);

        if (optionalDeviceToken.isPresent()) {
            // 1. 토큰이 이미 존재할 경우, 사용자 정보와 활성 상태를 갱신합니다.
            DeviceToken existingToken = optionalDeviceToken.get();
            existingToken.setUser(user);
            existingToken.setIsActive(true);
            deviceMapper.update(existingToken);
        } else {
            // 2. 토큰이 존재하지 않을 경우, 새로 생성하여 저장합니다.
            DeviceToken newDeviceToken = DeviceToken.builder()
                    .user(user)
                    .deviceType(requestDto.getDeviceType())
                    .token(requestDto.getToken())
                    .isActive(true)
                    .build();
            deviceMapper.save(newDeviceToken);
        }
    }
}

package com.app.nonstop.domain.device.controller;

import com.app.nonstop.domain.device.dto.DeviceTokenRequestDto;
import com.app.nonstop.domain.device.service.DeviceService;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Device", description = "디바이스 관련 API")
@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @Operation(summary = "FCM 토큰 등록/갱신", description = "클라이언트의 FCM 디바이스 토큰을 서버에 등록하거나 갱신합니다.", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/fcm-token")
    public ResponseEntity<ApiResponse<?>> registerDeviceToken(
            @Parameter(hidden = true)
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody @Valid DeviceTokenRequestDto requestDto
    ) {
        deviceService.registerOrUpdateDeviceToken(customUserDetails.getUserId(), requestDto);
        return ResponseEntity.ok(ApiResponse.success());
    }
}

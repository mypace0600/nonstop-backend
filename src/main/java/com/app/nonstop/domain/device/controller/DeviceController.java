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

/**
 * 디바이스 관련 API 요청을 처리하는 컨트롤러입니다.
 * FCM 디바이스 토큰 등록 및 갱신 기능을 제공합니다.
 */
@Tag(name = "Device", description = "디바이스 관련 API")
@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    /**
     * 클라이언트의 FCM 디바이스 토큰을 서버에 등록하거나 갱신합니다.
     * - 이미 존재하는 토큰이라면 사용자 정보와 활성 상태를 갱신하고,
     * - 존재하지 않는 토큰이라면 새로운 레코드를 생성합니다.
     *
     * @param customUserDetails Spring Security에서 주입한 현재 인증된 사용자 정보
     * @param requestDto 디바이스 타입과 FCM 토큰 정보를 포함하는 DTO
     * @return ApiResponse<?> 성공 응답 (데이터 없음)
     */
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

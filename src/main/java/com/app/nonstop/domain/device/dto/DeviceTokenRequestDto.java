package com.app.nonstop.domain.device.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "FCM 디바이스 토큰 등록/갱신 요청 DTO")
public class DeviceTokenRequestDto {

    @Schema(description = "디바이스 타입 (예: IOS, ANDROID)", example = "ANDROID")
    @NotBlank(message = "디바이스 타입을 입력해주세요.")
    private String deviceType;

    @Schema(description = "FCM 디바이스 토큰")
    @NotBlank(message = "디바이스 토큰을 입력해주세요.")
    private String token;
}

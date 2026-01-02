package com.app.nonstop.domain.device.entity;

import com.app.nonstop.domain.user.entity.User;
import com.app.nonstop.global.common.entity.BaseTimeEntity;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class DeviceToken extends BaseTimeEntity {

    private Long id;
    private User user;
    private String deviceType;
    private String token;
    private Boolean isActive;

    @Builder
    public DeviceToken(Long id, User user, String deviceType, String token, Boolean isActive) {
        this.id = id;
        this.user = user;
        this.deviceType = deviceType;
        this.token = token;
        this.isActive = isActive;
    }
}

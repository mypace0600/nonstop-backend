package com.app.nonstop.domain.user.entity;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginHistory {
    private Long id;
    private Long userId;
    private String type; // LOGIN, LOGOUT
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
}

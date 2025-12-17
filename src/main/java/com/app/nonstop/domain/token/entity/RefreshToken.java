package com.app.nonstop.domain.token.entity;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor
public class RefreshToken {

    private Long id;
    private Long userId;
    private String token;
    private Instant expiresAt;
    private Instant revokedAt;
    private Instant createdAt;

    @Builder
    public RefreshToken(Long userId, String token, Instant expiresAt) {
        this.userId = userId;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public void revoke() {
        this.revokedAt = Instant.now();
    }
}

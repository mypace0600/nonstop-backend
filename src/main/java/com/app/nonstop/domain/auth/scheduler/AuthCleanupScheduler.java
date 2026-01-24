package com.app.nonstop.domain.auth.scheduler;

import com.app.nonstop.domain.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthCleanupScheduler {

    private final AuthService authService;

    @Scheduled(cron = "0 0 3 * * *")  // 매일 새벽 3시 실행
    public void cleanupUnverifiedUsers() {
        log.info("Starting cleanup of unverified users...");
        authService.cleanupUnverifiedUsers();
        log.info("Finished cleanup of unverified users.");
    }
}

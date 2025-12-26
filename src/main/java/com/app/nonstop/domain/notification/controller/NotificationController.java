package com.app.nonstop.domain.notification.controller;

import com.app.nonstop.domain.notification.dto.NotificationDto;
import com.app.nonstop.domain.notification.service.NotificationService;
import com.app.nonstop.global.common.response.ApiResponse;
import com.app.nonstop.global.security.user.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ApiResponse<List<NotificationDto.Response>> getNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        return ApiResponse.success(notificationService.getMyNotifications(userDetails.getUserId()));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<?> readNotification(
            @PathVariable("id") Long id
    ) {
        notificationService.readNotification(id);
        return ApiResponse.success();
    }

    @PatchMapping("/read-all")
    public ApiResponse<?> readAll(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        notificationService.readAll(userDetails.getUserId());
        return ApiResponse.success();
    }
}

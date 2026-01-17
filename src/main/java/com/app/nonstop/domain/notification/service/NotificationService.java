package com.app.nonstop.domain.notification.service;

import com.app.nonstop.domain.device.service.DeviceService;
import com.app.nonstop.domain.notification.dto.NotificationDto;
import com.app.nonstop.domain.notification.entity.Notification;
import com.app.nonstop.domain.notification.entity.NotificationType;
import com.app.nonstop.mapper.NotificationMapper;
import com.google.firebase.messaging.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;
    private final DeviceService deviceService;
    private final FirebaseMessaging firebaseMessaging;

    /**
     * 알림을 생성합니다.
     * FCM 푸시 발송 로직이 포함되어 있습니다.
     */
    @Transactional
    public void createNotification(Long receiverId, Long actorId, String actorNickname, NotificationType type, String message, 
                                   Long postId, Long commentId, Long chatRoomId) {
        
        // 본인에게는 알림을 보내지 않음
        if (receiverId.equals(actorId)) {
            return;
        }

        Notification notification = Notification.builder()
                .userId(receiverId)
                .actorId(actorId)
                .actorNickname(actorNickname) // 닉네임 스냅샷
                .type(type)
                .message(message)
                .postId(postId)
                .commentId(commentId)
                .chatRoomId(chatRoomId)
                .isRead(false)
                .build();

        notificationMapper.insert(notification);
        
        // FCM Push Send Logic
        sendPushNotification(receiverId, type, message, notification);
    }

    private void sendPushNotification(Long receiverId, NotificationType type, String message, Notification notification) {
        try {
            List<String> deviceTokens = deviceService.getDeviceTokens(receiverId);
            
            if (deviceTokens == null || deviceTokens.isEmpty()) {
                log.info("No device tokens found for user: {}", receiverId);
                return;
            }

            MulticastMessage.Builder messageBuilder = MulticastMessage.builder()
                    .addAllTokens(deviceTokens)
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle("Nonstop") // 앱 이름 또는 상황에 맞는 제목
                            .setBody(message)
                            .build())
                    .putData("type", type.name())
                    .putData("notificationId", String.valueOf(notification.getId()));

            // 데이터 페이로드 추가 (null이 아닌 값만)
            if (notification.getPostId() != null) messageBuilder.putData("postId", String.valueOf(notification.getPostId()));
            if (notification.getCommentId() != null) messageBuilder.putData("commentId", String.valueOf(notification.getCommentId()));
            if (notification.getChatRoomId() != null) messageBuilder.putData("chatRoomId", String.valueOf(notification.getChatRoomId()));

            BatchResponse response = firebaseMessaging.sendEachForMulticast(messageBuilder.build());
            log.info("FCM sent successfully: success={}, failure={}", response.getSuccessCount(), response.getFailureCount());

            if (response.getFailureCount() > 0) {
                // 실패한 토큰에 대한 처리가 필요할 수 있음 (예: 유효하지 않은 토큰 삭제)
                // 현재는 로그만 남김
                response.getResponses().stream()
                        .filter(r -> !r.isSuccessful())
                        .forEach(r -> log.warn("FCM Send Error: {}", r.getException().getMessage()));
            }

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM message", e);
        } catch (Exception e) {
            log.error("Unknown error occurred while sending FCM message", e);
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationDto.Response> getMyNotifications(Long userId) {
        return notificationMapper.findAllByUserId(userId);
    }

    @Transactional
    public void readNotification(Long notificationId) {
        notificationMapper.updateRead(notificationId);
    }

    @Transactional
    public void readAll(Long userId) {
        notificationMapper.updateReadAll(userId);
    }
}

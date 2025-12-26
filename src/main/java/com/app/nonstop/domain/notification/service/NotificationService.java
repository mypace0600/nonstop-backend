package com.app.nonstop.domain.notification.service;

import com.app.nonstop.domain.notification.dto.NotificationDto;
import com.app.nonstop.domain.notification.entity.Notification;
import com.app.nonstop.domain.notification.entity.NotificationType;
import com.app.nonstop.domain.notification.mapper.NotificationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationMapper notificationMapper;

    /**
     * 알림을 생성합니다. (비동기 처리를 권장하지만, 일단 동기로 구현)
     * 추후 FCM 푸시 발송 로직도 여기에 추가될 수 있습니다.
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
        
        // TODO: FCM Push Send Logic Here
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

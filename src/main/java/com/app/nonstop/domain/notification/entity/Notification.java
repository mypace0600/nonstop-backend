package com.app.nonstop.domain.notification.entity;

import com.app.nonstop.global.common.entity.BaseTimeEntity;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Notification extends BaseTimeEntity {
    private Long id;
    private Long userId; // 수신자
    private Long actorId; // 행위자 (알림 유발자)
    private String actorNickname; // 행위자 닉네임 스냅샷
    private NotificationType type;
    private Long postId;
    private Long commentId;
    private Long chatRoomId;
    private String message;
    private Boolean isRead;
}

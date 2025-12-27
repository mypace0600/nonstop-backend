package com.app.nonstop.mapper;

import com.app.nonstop.domain.notification.dto.NotificationDto;
import com.app.nonstop.domain.notification.entity.Notification;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationMapper {
    void insert(Notification notification);
    
    List<NotificationDto.Response> findAllByUserId(@Param("userId") Long userId);
    
    void updateRead(@Param("id") Long id);
    
    void updateReadAll(@Param("userId") Long userId);
    
    long countUnread(@Param("userId") Long userId);
}

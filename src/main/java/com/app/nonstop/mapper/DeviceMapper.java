package com.app.nonstop.mapper;

import com.app.nonstop.domain.device.entity.DeviceToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface DeviceMapper {

    Optional<DeviceToken> findByToken(@Param("token") String token);

    void save(DeviceToken deviceToken);

    void update(DeviceToken deviceToken);

    void deleteAllByUserId(@Param("userId") Long userId);
}

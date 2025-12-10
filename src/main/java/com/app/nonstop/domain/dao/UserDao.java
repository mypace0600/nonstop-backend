package com.app.nonstop.domain.dao;

import com.app.nonstop.domain.dto.UserDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface UserDao {
    Optional<UserDto> findByEmail(@Param("email") String email);
    void save(UserDto user);
}

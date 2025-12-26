package com.app.nonstop.domain.user.mapper;

import com.app.nonstop.domain.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface UserMapper {
    Optional<User> findById(@Param("id") Long id);

    boolean existsByNickname(@Param("nickname") String nickname);

    void updateProfile(User user);

    void updatePassword(@Param("userId") Long userId, @Param("newPassword") String newPassword);

    void softDelete(@Param("userId") Long userId);

    Optional<User> findByEmail(@Param("email") String email);

    void insertUser(User user);

    void updateUser(User user);

    void updateProfileImage(@Param("userId") Long userId, @Param("profileImageUrl") String profileImageUrl);
}

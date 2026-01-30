package com.app.nonstop.mapper;

import com.app.nonstop.domain.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Mapper
public interface AuthMapper {
    Optional<User> findByEmail(@Param("email") String email);

    boolean existsByEmail(@Param("email") String email);

    boolean existsByNickname(@Param("nickname") String nickname);

    void save(User user);

    void updateEmailVerified(@Param("userId") Long userId,
                             @Param("emailVerified") boolean emailVerified,
                             @Param("emailVerifiedAt") LocalDateTime emailVerifiedAt);

    void updateBirthDate(@Param("userId") Long userId, @Param("birthDate") LocalDate birthDate);

    int deleteUnverifiedUsersBefore(@Param("threshold") LocalDateTime threshold);

    void updatePassword(@Param("userId") Long userId, @Param("password") String password);
}

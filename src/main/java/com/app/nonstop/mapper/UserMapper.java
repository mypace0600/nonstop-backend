package com.app.nonstop.mapper;

import com.app.nonstop.domain.user.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
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

    void updateUniversity(@Param("userId") Long userId, @Param("universityId") Long universityId, @Param("majorId") Long majorId);

    void updateVerificationStatus(@Param("userId") Long userId, @Param("isVerified") boolean isVerified, @Param("verificationMethod") com.app.nonstop.domain.user.entity.VerificationMethod verificationMethod);

    void updateBirthDate(@Param("userId") Long userId, @Param("birthDate") java.time.LocalDate birthDate);

    List<com.app.nonstop.domain.user.dto.UserResponseDto> searchByNickname(@Param("query") String query);
}

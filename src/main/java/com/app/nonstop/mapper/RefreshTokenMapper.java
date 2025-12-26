package com.app.nonstop.mapper;

import com.app.nonstop.domain.token.entity.RefreshToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Optional;

@Mapper
public interface RefreshTokenMapper {

    void save(RefreshToken refreshToken);

    Optional<RefreshToken> findByToken(@Param("token") String token);

    void deleteByUserId(@Param("userId") Long userId);
}

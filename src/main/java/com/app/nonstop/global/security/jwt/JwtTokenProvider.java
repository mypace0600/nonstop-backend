package com.app.nonstop.global.security.jwt;

import com.app.nonstop.global.security.user.CustomUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

 @Slf4j @Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final JwtParser jwtParser;
    private final long accessTokenExpireTime;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long accessTokenExpireTime
    ) {
        this.secretKey = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );

        this.jwtParser = Jwts.parser()
                .verifyWith(this.secretKey)
                .build();

        this.accessTokenExpireTime = accessTokenExpireTime;
    }

    /**
     * Access Token 생성
     */
    public String createAccessToken(Authentication authentication) {
        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpireTime);

        String authorities = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .subject(String.valueOf(userDetails.getUserId())) // subject는 String 권장
                .claim("auth", authorities)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 토큰 → Authentication 변환
     */
    public Authentication getAuthentication(String token) {
        Claims claims = jwtParser
                .parseSignedClaims(token)
                .getPayload();

        Long userId = Long.parseLong(claims.getSubject());

        List<GrantedAuthority> authorities =
                Collections.singletonList(
                        () -> claims.get("auth", String.class)
                );

        CustomUserDetails principal =
                new CustomUserDetails(userId, authorities);

        return new UsernamePasswordAuthenticationToken(
                principal,
                token,
                authorities
        );
    }

    /**
     * 토큰 유효성 검증
     */
    public boolean validateToken(String token) {
        try {
            jwtParser.parseSignedClaims(token);
            return true;

        } catch (SignatureException e) {
            log.warn("Invalid JWT signature", e);
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token", e);
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token", e);
        } catch (IllegalArgumentException e) {
            log.warn("JWT token is empty or null", e);
        }

        return false;
    }
}
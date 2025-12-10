package com.app.nonstop.config.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class JwtUtil {

    // Key 대신 구체적인 SecretKey 사용 (HMAC용)
    private final SecretKey signingKey;
    private final long expirationTime;
    private final long refreshTokenExpirationTime;

    public JwtUtil(@Value("${jwt.secret}") String secretKey,
                   @Value("${jwt.expiration}") long expirationTime,
                   @Value("${jwt.refresh-expiration}") long refreshTokenExpirationTime) {
        // 문자열 인코딩 명시 (UTF-8)
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret key must be at least 32 bytes for HS256");
        }
        // hmacShaKeyFor는 SecretKey 타입을 반환합니다.
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationTime = expirationTime;
        this.refreshTokenExpirationTime = refreshTokenExpirationTime;
    }

    public String generateToken(String email, List<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .subject(email) // setSubject -> subject (setter 접두어 제거 추세)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiryDate)
                // signWith(Key, Algorithm) -> Jwts.SIG.HS256 사용
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public String generateRefreshToken(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationTime);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    public List<String> extractRoles(String token) {
        Object rolesObject = extractAllClaims(token).get("roles");
        if (rolesObject instanceof List<?> rawList) {
            return rawList.stream()
                    .map(Object::toString)
                    .toList();
        }
        return List.of();
    }

    public Claims extractAllClaims(String token) {
        try {
            return Jwts.parser() // parserBuilder() -> parser()
                    .verifyWith(signingKey) // setSigningKey() -> verifyWith()
                    .build()
                    .parseSignedClaims(token) // parseClaimsJws() -> parseSignedClaims()
                    .getPayload(); // getBody() -> getPayload()
        } catch (ExpiredJwtException e) {
            // 만료된 토큰이어도 Claims를 일부 가져와야 할 때가 있다면 e.getClaims() 사용 가능
            throw new IllegalArgumentException("토큰이 만료되었습니다.");
        } catch (JwtException e) {
            throw new IllegalArgumentException("유효하지 않은 토큰입니다.");
        }
    }

    public void validateOrThrow(String token) {
        if (token == null) {
            throw new IllegalArgumentException("토큰이 존재하지 않습니다.");
        }
        // extractAllClaims 내부에서 이미 파싱 및 서명 검증을 수행함
        Claims claims = extractAllClaims(token);

        // 만료 시간은 parseSignedClaims 과정에서 이미 검사하지만, 명시적 체크를 원한다면 유지
        if (claims.getSubject() == null || claims.getExpiration().before(new Date())) {
            throw new IllegalArgumentException("토큰 정보가 유효하지 않습니다.");
        }
    }

    public boolean validateToken(String token) {
        try {
            // 파싱에 성공하면 서명과 만료시간이 모두 유효한 것임
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getTokenFromRequest(HttpServletRequest request) {
        // 1. Authorization 헤더
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            return bearer.substring(7);
        }

        // 2. 쿠키
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
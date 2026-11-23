package com.ecotvy.infrastructure.security;

import com.ecotvy.domain.user.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Access Token 발급/검증.
 * subject = userId, "role" claim = UserRole
 */
@Component
public class JwtTokenProvider {

    private static final String ROLE_CLAIM = "role";
    private static final String PASSWORD_CHANGE_CLAIM = "pwc";
    private static final int MIN_SECRET_BYTES = 32; // HS256 최소 키 길이 (256bit)

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration-ms:1800000}")
    private long accessTokenExpirationMs;

    private SecretKey key;

    @PostConstruct
    void init() {
        byte[] keyBytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "jwt.secret(JWT_SECRET 환경변수)는 최소 " + MIN_SECRET_BYTES + "바이트 이상이어야 합니다.");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(Long userId, UserRole role) {
        return createAccessToken(userId, role, false);
    }

    /**
     * @param passwordChangeRequired true면 비밀번호 변경 외의 API를 막는 claim을 넣는다.
     *                               (상태를 토큰에 담아 요청마다 DB를 보지 않고도 서버에서 강제)
     */
    public String createAccessToken(Long userId, UserRole role, boolean passwordChangeRequired) {
        Date now = new Date();
        var builder = Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim(ROLE_CLAIM, role.name())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + accessTokenExpirationMs));
        if (passwordChangeRequired) {
            builder.claim(PASSWORD_CHANGE_CLAIM, true);
        }
        return builder.signWith(key, SignatureAlgorithm.HS256).compact();
    }

    public boolean isPasswordChangeRequired(Claims claims) {
        return Boolean.TRUE.equals(claims.get(PASSWORD_CHANGE_CLAIM, Boolean.class));
    }

    /**
     * 토큰을 검증하고 claims를 반환한다.
     *
     * @throws io.jsonwebtoken.ExpiredJwtException 만료된 토큰
     * @throws io.jsonwebtoken.JwtException        서명 불일치/형식 오류 등
     */
    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public Long getUserId(Claims claims) {
        return Long.valueOf(claims.getSubject());
    }

    public UserRole getRole(Claims claims) {
        return UserRole.valueOf(claims.get(ROLE_CLAIM, String.class));
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }
}

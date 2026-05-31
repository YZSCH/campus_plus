package com.campus.trade.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Component
public class JwtTokenProvider {
    private final SecretKey secretKey;
    private final Set<String> invalidated = new HashSet<>();
    private static final long DEFAULT_MS = 2 * 3600 * 1000L;
    private static final long REMEMBER_MS = 7 * 24 * 3600 * 1000L;

    public JwtTokenProvider(@Value("${jwt.secret}") String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(Long userId, String username, String role, boolean rememberMe) {
        Date now = new Date();
        return Jwts.builder().setSubject(String.valueOf(userId))
                .claim("username", username).claim("role", role)
                .setIssuedAt(now).setExpiration(new Date(now.getTime() + (rememberMe ? REMEMBER_MS : DEFAULT_MS)))
                .signWith(secretKey).compact();
    }

    public Long getUserId(String token) { return Long.parseLong(parse(token).getSubject()); }
    public String getRole(String token) { return parse(token).get("role", String.class); }

    public boolean validate(String token) {
        try { return !invalidated.contains(token) && parse(token) != null; }
        catch (JwtException e) { return false; }
    }

    public void invalidate(String token) { invalidated.add(token); }

    private Claims parse(String token) {
        return Jwts.parserBuilder().setSigningKey(secretKey).build().parseClaimsJws(token).getBody();
    }
}

package org.kosign.chatbotapi.config.security;


import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.kosign.chatbotapi.domains.SecurityUser;
import org.kosign.chatbotapi.enums.Role;
import org.kosign.chatbotapi.properties.JwtProperties;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtConfig;
    private final JwtDecoder jwtDecoder;

    public long getExpireIn() {
        return jwtConfig.expiration().getSeconds();
    }

    public String extractUsername(String token) {
        return jwtDecoder.decode(token).getSubject();
    }

    private Instant extractExpiration(String token) {
        return jwtDecoder.decode(token).getExpiresAt();
    }

    public String extractToken(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            return token.substring(7); // Remove "Bearer " prefix
        }
        return null;
    }

    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).isBefore(Instant.now());
        } catch (ExpiredJwtException e) {
            LoggerFactory.getLogger(JwtUtil.class).warn("Expired JWT token: {}", e.getMessage()); // Changed to warn
            return true; // Token is expired
        } catch (JwtException e) {
            LoggerFactory.getLogger(JwtUtil.class).error("Invalid JWT token: {}", e.getMessage());
            return true; // Token is invalid
        }
    }

    public String doGenerateToken(SecurityUser securityUser) {
        Instant instant = Instant.now();

            Map<String, Object> claims = new HashMap<>();
            claims.put("id", Objects.requireNonNull(securityUser.getUserId(), "User ID cannot be null"));
            claims.put("username", Objects.requireNonNull(securityUser.getUsername(), "Username cannot be null"));
            // claims.put("role", Objects.requireNonNull(securityUser.getAuthorities(), "Authorities cannot be null"));

            JwtClaimsSet jwtClaimsSet = JwtClaimsSet.builder()
                    .subject(securityUser.getUsername())
                    .issuer(Role.USER.name()) // Assuming issuer is the role of the user
                    .claims(c ->c.putAll(claims))
                    .issuedAt(instant)
                    .expiresAt(instant.plus(jwtConfig.expiration().getSeconds(), ChronoUnit.SECONDS))
                    .build();
            return jwtEncoder.encode(JwtEncoderParameters.from(jwtClaimsSet)).getTokenValue();

    }
}
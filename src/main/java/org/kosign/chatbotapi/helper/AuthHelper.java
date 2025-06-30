package org.kosign.chatbotapi.helper;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public class AuthHelper {

    private static Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private static Jwt getJwt() {
        Authentication auth = getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }
        throw new IllegalStateException("JWT not found in security context.");
    }

    public static Long getUserId() {
        Jwt jwt = getJwt();
        String userId = jwt.getClaimAsString("id"); // Ensure "id" is present in JWT
        if (userId == null) {
            throw new IllegalStateException("JWT does not contain 'id' claim.");
        }
        return Long.parseLong(userId);
    }

    public static String getUsername() {
        Jwt jwt = getJwt();
        return jwt.getClaimAsString("username") != null ? jwt.getClaimAsString("username") : jwt.getSubject();
    }

    public static String getRole() {
        Jwt jwt = getJwt();
        return jwt.getClaimAsString("role");
    }
}

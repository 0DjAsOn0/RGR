package com.rgr.messanger.web.security;

import com.rgr.messanger.entity.user.Role;
import com.rgr.messanger.entity.user.User;
import com.rgr.messanger.exception.AccessDeniedException;
import com.rgr.messanger.exception.EmailVerificationException;
import com.rgr.messanger.service.UserService;
import com.rgr.messanger.web.dto.auth.JwtResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenProvider {

    private static final String CLAIM_ID    = "id";
    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_TYPE  = "type";
    private static final String TYPE_EMAIL_VERIFICATION = "email-verification";

    private final JwtProperties jwtProperties;
    private final UserDetailsService userDetailsService;
    private final UserService userService;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes());
    }

    // ========================
    // СОЗДАНИЕ ТОКЕНОВ
    // ========================

    public String createAccessToken(Long userId, String username, Set<Role> roles) {
        Claims claims = Jwts.claims()
                .subject(username)
                .add(CLAIM_ID, userId)
                .add(CLAIM_ROLES, resolveRoles(roles))
                .build();

        Instant validity = Instant.now()
                .plus(jwtProperties.getAccess(), ChronoUnit.HOURS);

        return Jwts.builder()
                .claims(claims)
                .expiration(Date.from(validity))
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(Long userId, String username) {
        Claims claims = Jwts.claims()
                .subject(username)
                .add(CLAIM_ID, userId)
                .build();

        Instant validity = Instant.now()
                .plus(jwtProperties.getRefresh(), ChronoUnit.DAYS);

        return Jwts.builder()
                .claims(claims)
                .expiration(Date.from(validity))
                .signWith(key)
                .compact();
    }

    public String generateVerificationToken(String email) {
        Claims claims = Jwts.claims()
                .subject(email)
                .add(CLAIM_TYPE, TYPE_EMAIL_VERIFICATION)
                .build();

        Instant validity = Instant.now().plus(24, ChronoUnit.HOURS);

        return Jwts.builder()
                .claims(claims)
                .expiration(Date.from(validity))
                .signWith(key)
                .compact();
    }

    // ========================
    // REFRESH
    // ========================

    public JwtResponse refreshUserTokens(String refreshToken) {
        if (!isValid(refreshToken)) {
            throw new AccessDeniedException("Refresh-токен невалиден или просрочен");
        }

        Long userId = getId(refreshToken);
        User user   = userService.getById(userId);

        JwtResponse jwtResponse = new JwtResponse();
        jwtResponse.setId(userId);
        jwtResponse.setUsername(user.getUsername());
        jwtResponse.setAccessToken(
                createAccessToken(userId, user.getUsername(), user.getRoles())
        );
        jwtResponse.setRefreshToken(
                createRefreshToken(userId, user.getUsername())
        );
        return jwtResponse;
    }

    // ========================
    // ВАЛИДАЦИЯ
    // ========================

    /**
     * Безопасная проверка токена.
     * Возвращает false при любой ошибке (подделка, истёкший, битый).
     */
    public boolean isValid(String token) {
        if (token == null || token.isBlank()) return false;

        try {
            Claims claims = parseClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    // ========================
    // ИЗВЛЕЧЕНИЕ ДАННЫХ
    // ========================

    public Authentication getAuthentication(String token) {
        String username = getUsername(token);
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        return new UsernamePasswordAuthenticationToken(
                userDetails,
                "",
                userDetails.getAuthorities()
        );
    }

    public String getEmailFromVerificationToken(String token) {
        Claims claims = parseClaims(token);

        if (!TYPE_EMAIL_VERIFICATION.equals(claims.get(CLAIM_TYPE))) {
            throw new EmailVerificationException("Неверный тип токена");
        }

        return claims.getSubject();
    }

    // ========================
    // PRIVATE
    // ========================

    private Long getId(String token) {
        Object id = parseClaims(token).get(CLAIM_ID);
        if (id == null) return null;
        // id может быть Integer / Long / String — берём через toString
        return Long.valueOf(id.toString());
    }

    private String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private List<String> resolveRoles(Set<Role> roles) {
        return roles.stream()
                .map(Enum::name)
                .toList();
    }

    public List<String> getRoles(String token) {
        try {
            Object roles = parseClaims(token).get(CLAIM_ROLES);
            if (roles instanceof List<?> list) {
                return list.stream()
                        .filter(java.util.Objects::nonNull)
                        .map(Object::toString)
                        .toList();
            }
            return List.of();
        } catch (Exception e) {
            return List.of();
        }
    }
}
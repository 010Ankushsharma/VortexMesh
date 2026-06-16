package com.vortexmesh.auth.service;

import com.vortexmesh.auth.model.ApiKey;
import com.vortexmesh.auth.model.User;
import com.vortexmesh.common.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;
    @PersistenceContext
    private EntityManager entityManager;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public String authenticate(String username, String password) {
        // Simplified - in production use UserDetailsService
        User user = entityManager.createQuery(
            "SELECT u FROM User u WHERE u.username = :username", User.class)
            .setParameter("username", username)
            .getSingleResult();

        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        Map<String, Object> claims = Map.of(
            "roles", user.getRoles(),
            "tenantId", user.getTenantId() != null ? user.getTenantId() : "",
            "userId", user.getId()
        );

        String token = jwtTokenProvider.generateToken(username, claims);

        // Cache token in Redis
        redisTemplate.opsForValue().set("vortex:token:" + token, user.getId(), Duration.ofHours(1));

        log.info("User authenticated: {}", username);
        return token;
    }

    @Transactional
    public String registerUser(String username, String password, String email, String tenantId) {
        User user = User.builder()
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .email(email)
                .tenantId(tenantId)
                .roles(Set.of("USER"))
                .enabled(true)
                .createdAt(Instant.now())
                .build();

        entityManager.persist(user);
        log.info("User registered: {}", username);
        return user.getId();
    }

    @Transactional
    public String generateApiKey(String userId, String name, String tenantId) {
        String keyValue = "vortex_" + UUID.randomUUID().toString().replace("-", "");

        ApiKey apiKey = ApiKey.builder()
                .keyValue(keyValue)
                .name(name)
                .userId(userId)
                .tenantId(tenantId)
                .enabled(true)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(Duration.ofDays(365)))
                .build();

        entityManager.persist(apiKey);

        // Cache API key
        redisTemplate.opsForValue().set("vortex:apikey:" + keyValue, userId, Duration.ofDays(365));

        return keyValue;
    }

    public boolean validateToken(String token) {
        return jwtTokenProvider.isTokenValid(token);
    }
}

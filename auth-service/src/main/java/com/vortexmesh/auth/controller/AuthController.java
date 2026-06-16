package com.vortexmesh.auth.controller;

import com.vortexmesh.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> credentials) {
        String token = authService.authenticate(
            credentials.get("username"), credentials.get("password"));
        return ResponseEntity.ok(Map.of(
            "token", token,
            "type", "Bearer",
            "expiresIn", 3600
        ));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> request) {
        String userId = authService.registerUser(
            request.get("username"), request.get("password"), 
            request.get("email"), request.get("tenantId"));
        return ResponseEntity.ok(Map.of("userId", userId, "status", "created"));
    }

    @PostMapping("/api-keys")
    public ResponseEntity<Map<String, Object>> createApiKey(
            @RequestBody Map<String, String> request,
            @RequestHeader("X-Authenticated-User") String userId) {
        String apiKey = authService.generateApiKey(userId, request.get("name"), request.get("tenantId"));
        return ResponseEntity.ok(Map.of("apiKey", apiKey, "name", request.get("name")));
    }

    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestHeader("Authorization") String auth) {
        String token = auth.replace("Bearer ", "");
        boolean valid = authService.validateToken(token);
        return ResponseEntity.ok(Map.of("valid", valid));
    }
}

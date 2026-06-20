package com.vortexmesh.sample.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@SpringBootApplication
@RestController
@RequestMapping("/api/v1/users")
public class UserServiceApplication {

    private final Map<String, Map<String, Object>> users = new HashMap<>();

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

    @GetMapping
    public List<Map<String, Object>> getUsers() {
        return new ArrayList<>(users.values());
    }

    @GetMapping("/{id}")
    public Map<String, Object> getUser(@PathVariable String id) {
        return users.getOrDefault(id, Map.of("error", "not found"));
    }

    @PostMapping
    public Map<String, Object> createUser(@RequestBody Map<String, Object> user) {
        String id = UUID.randomUUID().toString();
        user.put("id", id);
        users.put(id, user);
        return user;
    }
}
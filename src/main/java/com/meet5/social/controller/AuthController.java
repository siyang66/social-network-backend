package com.meet5.social.controller;

import com.meet5.social.common.Result;
import com.meet5.social.config.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final JwtUtil jwtUtil;

    public AuthController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    // minimal login — in production this would validate credentials against the users table
    @PostMapping("/login")
    public ResponseEntity<Result<Map<String, String>>> login(@RequestBody Map<String, Object> body) {
        Long userId = Long.parseLong(body.get("userId").toString());
        String token = jwtUtil.generateToken(userId);
        return ResponseEntity.ok(Result.ok(Map.of("token", token)));
    }
}

package org.example.security;

import org.example.DataBaseRepositories.UserRepository;
import org.example.LoginRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Autowired
    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody(required = false) LoginRequest loginRequest) {
        if (loginRequest == null || loginRequest.getUsername() == null || loginRequest.getPassword() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Missing username or password"));
        }
        return userRepository.findByUsername(loginRequest.getUsername())
                .map(user -> {
                    if (passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                        String token = jwtUtil.generateToken(user.getUsername());
                        return ResponseEntity.ok(Map.of(
                                "success", true,
                                "message", "Login successful!",
                                "token", token,
                                "role", user.getRole().name()
                        ));
                    } else {
                        return ResponseEntity.status(401)
                                .body(Map.of("success", false, "message", "Invalid credentials"));
                    }
                })
                .orElse(ResponseEntity.status(401)
                        .body(Map.of("success", false, "message", "User not found")));
    }
}
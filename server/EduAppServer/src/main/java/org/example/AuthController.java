package org.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // Dodaj to pole

    // Wstrzyknij zależności przez konstruktor
    @Autowired
    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder; // Inicjalizacja
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        return userRepository.findByUsername(loginRequest.getUsername())
                .map(user -> {
                    if (passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                        return ResponseEntity.ok(Map.of(
                                "success", true,
                                "message", "Login successful!",
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
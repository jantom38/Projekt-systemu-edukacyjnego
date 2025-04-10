package org.example;

import org.example.LoginRequest;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        if ("admin".equals(loginRequest.getUsername()) && "password123".equals(loginRequest.getPassword())) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Login successful!"));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "Invalid credentials"));
        }
    }
}

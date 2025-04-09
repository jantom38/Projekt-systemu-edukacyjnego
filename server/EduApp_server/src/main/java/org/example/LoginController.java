package com.example.loginserver.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class LoginController {

    // Tymczasowe "baza danych" użytkowników
    private final Map<String, String> users = new HashMap<>();

    public LoginController() {
        // Dodajemy testowego użytkownika
        users.put("admin", "admin123");
        users.put("user", "password");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String storedPassword = users.get(request.getUsername());

        if (storedPassword != null && storedPassword.equals(request.getPassword())) {
            // Logowanie poprawne
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Login successful");
            return ResponseEntity.ok(response);
        } else {
            // Logowanie niepoprawne
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Invalid username or password");
            return ResponseEntity.status(401).body(response);
        }
    }

    // Klasa pomocnicza do przechwytywania danych logowania
    private static class LoginRequest {
        private String username;
        private String password;

        // Gettery i settery
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
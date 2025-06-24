package org.example.security;

import org.example.DataBaseRepositories.UserRepository;
import org.example.LoginRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Kontroler odpowiedzialny za uwierzytelnianie użytkowników w systemie.
 * Obsługuje żądania logowania i generowania tokenów JWT.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /**
     * Repozytorium do zarządzania operacjami na bazie danych użytkowników.
     */
    private final UserRepository userRepository;
    /**
     * Koder haseł do bezpiecznego przechowywania i weryfikacji haseł.
     */
    private final PasswordEncoder passwordEncoder;
    /**
     * Narzędzie do generowania i walidacji tokenów JWT.
     */
    private final JwtUtil jwtUtil;

    /**
     * Konstruktor klasy AuthController.
     * Wstrzykuje zależności UserRepository, PasswordEncoder i JwtUtil.
     *
     * @param userRepository Repozytorium użytkowników.
     * @param passwordEncoder Koder haseł.
     * @param jwtUtil Narzędzie do obsługi JWT.
     */
    @Autowired
    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Obsługuje żądanie logowania użytkownika.
     * Sprawdza podane dane uwierzytelniające i w przypadku powodzenia, zwraca token JWT oraz rolę użytkownika.
     *
     * @param loginRequest Obiekt zawierający nazwę użytkownika i hasło.
     * @return ResponseEntity zawierający status operacji, wiadomość, token JWT i rolę użytkownika w przypadku sukcesu,
     * lub status błędu z odpowiednią wiadomością w przypadku niepowodzenia.
     */
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
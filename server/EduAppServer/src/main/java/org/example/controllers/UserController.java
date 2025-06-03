package org.example.controllers;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.example.CodeValidity;
import org.example.DataBaseRepositories.*;
import org.example.database.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/courses")
public class UserController {

    private final UserRepository userRepository;
    private final RoleCodeRepository roleCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserCourseRepository userCourseRepository;
    private final QuizResultRepository quizResultRepository;
    private final QuizAnswerRepository quizAnswerRepository;

    @Autowired
    public UserController(UserRepository userRepository,
                          RoleCodeRepository roleCodeRepository,
                          PasswordEncoder passwordEncoder,
                          UserCourseRepository userCourseRepository,
                          QuizResultRepository quizResultRepository,
                          QuizAnswerRepository quizAnswerRepository) {
        this.userRepository = userRepository;
        this.roleCodeRepository = roleCodeRepository;
        this.passwordEncoder = passwordEncoder;
        this.userCourseRepository = userCourseRepository;
        this.quizResultRepository = quizResultRepository;
        this.quizAnswerRepository = quizAnswerRepository;
    }

    @PostMapping("/auth/generate-student-code")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> generateStudentCode(@RequestBody Map<String, String> request) {
        log.info("Próba wygenerowania kodu dla roli STUDENT przez nauczyciela {}", Utils.currentUsername());

        String validityStr = request.get("validity");
        if (validityStr == null || validityStr.isBlank()) {
            log.warn("Brak parametru validity w żądaniu");
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Parametr validity jest wymagany"));
        }

        CodeValidity validity;
        try {
            validity = CodeValidity.fromValue(validityStr);
        } catch (IllegalArgumentException e) {
            log.warn("Nieprawidłowa wartość validity: {}", validityStr);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Nieprawidłowa wartość validity. Dozwolone: 1_HOUR, 2_HOURS, 1_DAY, 1_WEEK"));
        }

        User teacher = userRepository.findByUsername(Utils.currentUsername())
                .orElseThrow(() -> new RuntimeException("Nauczyciel nie znaleziony"));

        String code;
        int attempts = 0;
        final int maxAttempts = 10;
        do {
            code = Utils.generateRandomCode();
            attempts++;
            if (attempts > maxAttempts) {
                log.error("Nie udało się wygenerować unikalnego kodu po {} próbach", maxAttempts);
                return ResponseEntity.internalServerError()
                        .body(Map.of("success", false, "message", "Nie udało się wygenerować unikalnego kodu"));
            }
        } while (roleCodeRepository.findByCodeAndIsActiveTrue(code).isPresent());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(validity.getHours());

        RoleCode roleCode = new RoleCode(
                code,
                UserRole.STUDENT,
                teacher,
                now,
                expiresAt
        );

        roleCodeRepository.save(roleCode);
        log.info("Wygenerowano kod {} dla roli STUDENT, ważny do {}", code, expiresAt);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Kod dla studenta wygenerowany pomyślnie",
                "code", code,
                "expiresAt", expiresAt.toString()
        ));
    }

    @PostMapping("/auth/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> request) {
        log.info("Próba rejestracji użytkownika");

        String username = request.get("username");
        String password = request.get("password");
        String roleCode = request.get("roleCode");

        if (username == null || username.isBlank() || password == null || password.isBlank() || roleCode == null || roleCode.isBlank()) {
            log.warn("Nieprawidłowe dane rejestracji: brak nazwy użytkownika, hasła lub kodu roli");
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Nazwa użytkownika, hasło i kod roli są wymagane"));
        }

        if (userRepository.findByUsername(username).isPresent()) {
            log.warn("Nazwa użytkownika {} jest już zajęta", username);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Nazwa użytkownika jest już zajęta"));
        }

        return roleCodeRepository.findByCodeAndIsActiveTrue(roleCode)
                .map(code -> {
                    if (code.getExpiresAt().isBefore(LocalDateTime.now())) {
                        log.warn("Kod {} wygasł", roleCode);
                        return ResponseEntity.badRequest()
                                .body(Map.of("success", false, "message", "Kod wygasł"));
                    }

                    if (code.getRole() != UserRole.STUDENT) {
                        log.warn("Kod {} nie jest przeznaczony dla roli STUDENT", roleCode);
                        return ResponseEntity.badRequest()
                                .body(Map.of("success", false, "message", "Nieprawidłowy kod roli"));
                    }

                    User user = new User();
                    user.setUsername(username);
                    user.setPassword(passwordEncoder.encode(password));
                    user.setRole(UserRole.STUDENT);
                    userRepository.save(user);

                    code.setActive(false);
                    roleCodeRepository.save(code);

                    log.info("Użytkownik {} zarejestrowany pomyślnie z rolą STUDENT", username);
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Rejestracja pomyślna"
                    ));
                })
                .orElseGet(() -> {
                    log.warn("Nieprawidłowy lub nieaktywny kod roli: {}", roleCode);
                    return ResponseEntity.badRequest()
                            .body(Map.of("success", false, "message", "Nieprawidłowy lub nieaktywny kod roli"));
                });
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAllUsers() {
        log.info("Pobieranie listy wszystkich użytkowników przez admina {}", Utils.currentUsername());
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> userList = users.stream()
                .map(user -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", user.getId());
                    userMap.put("username", user.getUsername());
                    userMap.put("role", user.getRole().name());
                    return userMap;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "users", userList
        ));
    }

    @PostMapping("/users/{userId}/promote-to-teacher")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> promoteToTeacher(@PathVariable Long userId) {
        log.info("Próba nadania roli TEACHER użytkownikowi ID: {} przez admina {}", userId, Utils.currentUsername());
        return userRepository.findById(userId)
                .map(user -> {
                    if (user.getRole() == UserRole.ADMIN) {
                        log.warn("Próba zmiany roli ADMIN dla użytkownika ID: {}", userId);
                        return ResponseEntity.badRequest()
                                .body(Map.of("success", false, "message", "Nie można zmienić roli ADMIN"));
                    }
                    if (user.getRole() == UserRole.TEACHER) {
                        log.info("Użytkownik ID: {} jest już nauczycielem", userId);
                        return ResponseEntity.ok(Map.of(
                                "success", true,
                                "message", "Użytkownik jest już nauczycielem"
                        ));
                    }
                    user.setRole(UserRole.TEACHER);
                    userRepository.save(user);
                    log.info("Użytkownik ID: {} otrzymał rolę TEACHER", userId);
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Użytkownik otrzymał rolę nauczyciela"
                    ));
                })
                .orElseGet(() -> {
                    log.error("Użytkownik ID: {} nie znaleziony", userId);
                    return ResponseEntity.status(404)
                            .body(Map.of("success", false, "message", "Użytkownik nie znaleziony"));
                });
    }

    @PostMapping("/users/{userId}/demote-to-student")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> demoteToStudent(@PathVariable Long userId) {
        log.info("Próba zmiany roli na STUDENT dla użytkownika ID: {} przez admina {}", userId, Utils.currentUsername());
        return userRepository.findById(userId)
                .map(user -> {
                    if (user.getRole() == UserRole.ADMIN) {
                        log.warn("Próba zmiany roli ADMIN dla użytkownika ID: {}", userId);
                        return ResponseEntity.badRequest()
                                .body(Map.of("success", false, "message", "Nie można zmienić roli ADMIN"));
                    }
                    if (user.getRole() == UserRole.STUDENT) {
                        log.info("Użytkownik ID: {} jest już studentem", userId);
                        return ResponseEntity.ok(Map.of(
                                "success", true,
                                "message", "Użytkownik jest już studentem"
                        ));
                    }
                    user.setRole(UserRole.STUDENT);
                    userRepository.save(user);
                    log.info("Użytkownik ID: {} otrzymał rolę STUDENT", userId);
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Użytkownik otrzymał rolę studenta"
                    ));
                })
                .orElseGet(() -> {
                    log.error("Użytkownik ID: {} nie znaleziony", userId);
                    return ResponseEntity.status(404)
                            .body(Map.of("success", false, "message", "Użytkownik nie znaleziony"));
                });
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        String adminUsername = Utils.currentUsername();
        log.info("Administrator {} próbuje usunąć użytkownika ID: {}", adminUsername, userId);

        User userToDelete = userRepository.findById(userId)
                .orElse(null);

        if (userToDelete == null) {
            log.warn("Użytkownik ID: {} nie znaleziony do usunięcia.", userId);
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "Użytkownik nie znaleziony"));
        }

        User adminUser = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono aktualnie zalogowanego administratora"));

        if (userToDelete.getId().equals(adminUser.getId())) {
            log.warn("Administrator {} próbował usunąć samego siebie.", adminUsername);
            return ResponseEntity.status(403)
                    .body(Map.of("success", false, "message", "Administrator nie może usunąć samego siebie."));
        }

        if (userToDelete.getRole() == UserRole.ADMIN) {
            log.warn("Administrator {} próbował usunąć innego administratora (ID: {}).", adminUsername, userId);
            return ResponseEntity.status(403)
                    .body(Map.of("success", false, "message", "Nie można usunąć innego administratora."));
        }

        List<UserCourse> userCourses = userCourseRepository.findByUserId(userId);
        if (!userCourses.isEmpty()) {
            userCourseRepository.deleteAllInBatch(userCourses);
            log.info("Usunięto {} przypisań do kursów (UserCourse) dla użytkownika ID: {}", userCourses.size(), userId);
        }

        List<QuizResult> quizResultsForUser = quizResultRepository.findByUserId(userId);
        if (quizResultsForUser != null && !quizResultsForUser.isEmpty()) {
            log.info("Znaleziono {} wyników quizów (QuizResult) dla użytkownika ID: {}. Rozpoczynanie usuwania powiązanych odpowiedzi (QuizAnswer)...", quizResultsForUser.size(), userId);
            for (QuizResult result : quizResultsForUser) {
                quizAnswerRepository.deleteByQuizResultId(result.getId());
                log.debug("Usunięto odpowiedzi (QuizAnswer) dla QuizResult ID: {}", result.getId());
            }
            log.info("Zakończono usuwanie odpowiedzi (QuizAnswer) dla wyników quizów użytkownika ID: {}", userId);
        } else {
            log.info("Nie znaleziono wyników quizów (QuizResult) dla użytkownika ID: {} do przetworzenia.", userId);
        }

        userRepository.delete(userToDelete);
        log.info("Użytkownik ID: {} został pomyślnie usunięty przez administratora {}", userId, adminUsername);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Użytkownik i wszystkie jego powiązane dane zostały usunięte pomyślnie."
        ));
    }
}
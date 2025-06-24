
package org.example.controllers;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.example.DataBaseRepositories.*;
import org.example.database.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @brief Kontroler odpowiedzialny za zarządzanie quizami w ramach kursów w systemie.
 *
 * Udostępnia endpointy do pobierania, dodawania, aktualizowania i usuwania quizów,
 * a także do pobierania statystyk quizów.
 */
@Slf4j
@RestController
@RequestMapping("/api/courses")
public class QuizController {

    private final CourseRepository courseRepository;
    private final QuizRepository quizRepository;
    private final QuizResultRepository quizResultRepository;
    private final UserCourseRepository userCourseRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final UserRepository userRepository;

    /**
     * @brief Konstruktor klasy QuizController.
     * @param courseRepository Repozytorium kursów.
     * @param quizRepository Repozytorium quizów.
     * @param quizResultRepository Repozytorium wyników quizów.
     * @param userCourseRepository Repozytorium powiązań użytkowników z kursami.
     * @param quizQuestionRepository Repozytorium pytań quizowych.
     * @param userRepository Repozytorium użytkowników.
     */
    @Autowired
    public QuizController(CourseRepository courseRepository,
                          QuizRepository quizRepository,
                          QuizResultRepository quizResultRepository,
                          UserCourseRepository userCourseRepository,
                          QuizQuestionRepository quizQuestionRepository,
                          UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.quizRepository = quizRepository;
        this.quizResultRepository = quizResultRepository;
        this.userCourseRepository = userCourseRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.userRepository = userRepository;
    }

    /**
     * @brief Pobiera wszystkie quizy dla danego kursu.
     * @param id ID kursu.
     * @return ResponseEntity z wynikiem operacji:
     * - success (boolean) - czy operacja się powiodła
     * - message (String) - komunikat o błędzie (jeśli dotyczy)
     * - quizzes (List<Quiz>) - lista quizów (w przypadku sukcesu)
     */
    @GetMapping("/{id}/quizzes")
    public ResponseEntity<?> getCourseQuizzes(@PathVariable Long id) {
        Authentication auth = Utils.getAuthentication();
        if (Utils.isTeacher(auth)) {
            boolean owns = courseRepository.findByIdAndTeacherUsername(id, Utils.currentUsername()).isPresent();
            if (!owns) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "message", "Brak dostępu do tego kursu"));
            }
        } else if (Utils.isAdmin(auth)) {
            log.info("Admin {} uzyskuje dostęp do quizów kursu ID: {}", Utils.currentUsername(), id);
        } else {
            Long userId = Utils.getCurrentUserId(userRepository);
            if (!userCourseRepository.existsByUserIdAndCourseId(userId, id)) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "message", "Nie jesteś zapisany na ten kurs"));
            }
        }

        if (!courseRepository.existsById(id)) {
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "Kurs nie znaleziony"));
        }

        List<Quiz> quizzes = quizRepository.findByCourseId(id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "quizzes", quizzes
        ));
    }

    /**
     * @brief Dodaje nowy quiz do określonego kursu.
     * @param id ID kursu.
     * @param quiz Obiekt Quiz zawierający dane quizu.
     * @return ResponseEntity z wynikiem operacji:
     * - success (boolean) - czy operacja się powiodła
     * - message (String) - komunikat o wyniku
     * - quiz (Quiz) - dodany quiz (w przypadku sukcesu)
     */
    @PostMapping("/{id}/quizzes")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<?> addQuiz(@PathVariable Long id, @RequestBody Quiz quiz) {
        if (quiz.getTitle() == null || quiz.getTitle().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Tytuł quizu jest wymagany"));
        }
        if (quiz.getNumberOfQuestionsToDisplay() <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Ilość pytań do wyświetlenia musi być większa niż 0"));
        }

        return courseRepository.findById(id)
                .map(course -> {
                    if (Utils.isTeacher(Utils.getAuthentication()) &&
                            !course.getTeacher().getUsername().equals(Utils.currentUsername())) {
                        return ResponseEntity.status(403)
                                .body(Map.of("success", false, "message", "Brak dostępu do tego kursu"));
                    }
                    quiz.setCourse(course);
                    Quiz savedQuiz = quizRepository.save(quiz);
                    log.info("Dodano quiz '{}' (ID: {}) do kursu ID: {}", quiz.getTitle(), savedQuiz.getId(), id);
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Quiz dodany pomyślnie",
                            "quiz", savedQuiz
                    ));
                })
                .orElse(ResponseEntity.status(404)
                        .body(Map.of("success", false, "message", "Kurs nie istnieje")));
    }

    /**
     * @brief Aktualizuje istniejący quiz.
     * @param quizId ID quizu do zaktualizowania.
     * @param quiz Obiekt Quiz zawierający zaktualizowane dane.
     * @return ResponseEntity z wynikiem operacji:
     * - success (boolean) - czy operacja się powiodła
     * - message (String) - komunikat o wyniku
     * - quiz (Quiz) - zaktualizowany quiz (w przypadku sukcesu)
     */
    @PutMapping("/quizzes/{quizId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<?> updateQuiz(@PathVariable Long quizId, @RequestBody Quiz quiz) {
        if (quiz.getTitle() == null || quiz.getTitle().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Tytuł quizu jest wymagany"));
        }
        if (quiz.getNumberOfQuestionsToDisplay() <= 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Ilość pytań do wyświetlenia musi być większa niż 0"));
        }

        return quizRepository.findById(quizId)
                .map(existingQuiz -> {
                    Course course = existingQuiz.getCourse();
                    if (Utils.isTeacher(Utils.getAuthentication()) &&
                            !course.getTeacher().getUsername().equals(Utils.currentUsername())) {
                        return ResponseEntity.status(403)
                                .body(Map.of("success", false, "message", "Brak dostępu do tego quizu"));
                    }
                    existingQuiz.setTitle(quiz.getTitle());
                    existingQuiz.setDescription(quiz.getDescription());
                    existingQuiz.setNumberOfQuestionsToDisplay(quiz.getNumberOfQuestionsToDisplay());
                    Quiz updatedQuiz = quizRepository.save(existingQuiz);
                    log.info("Zaktualizowano quiz '{}' (ID: {})", updatedQuiz.getTitle(), quizId);
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Quiz zaktualizowany pomyślnie",
                            "quiz", updatedQuiz
                    ));
                })
                .orElse(ResponseEntity.status(404)
                        .body(Map.of("success", false, "message", "Quiz nie znaleziony")));
    }

    /**
     * @brief Usuwa quiz o podanym identyfikatorze.
     * @param quizId ID quizu do usunięcia.
     * @return ResponseEntity z wynikiem operacji:
     * - success (boolean) - czy operacja się powiodła
     * - message (String) - komunikat o wyniku
     */
    @DeleteMapping("/quizzes/{quizId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Transactional
    public ResponseEntity<?> deleteQuiz(@PathVariable Long quizId) {
        return quizRepository.findById(quizId)
                .map(quiz -> {
                    Course course = quiz.getCourse();
                    if (Utils.isTeacher(Utils.getAuthentication()) &&
                            !course.getTeacher().getUsername().equals(Utils.currentUsername())) {
                        return ResponseEntity.status(403)
                                .body(Map.of("success", false, "message", "Brak dostępu do tego quizu"));
                    }
                    quizQuestionRepository.deleteByQuizId(quizId);
                    quizRepository.delete(quiz);
                    log.info("Usunięto quiz ID: {}", quizId);
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Quiz usunięty pomyślnie"
                    ));
                })
                .orElse(ResponseEntity.status(404)
                        .body(Map.of("success", false, "message", "Quiz nie znaleziony")));
    }

    /**
     * @brief Pobiera statystyki dla wszystkich quizów w danym kursie.
     * @param courseId ID kursu.
     * @return ResponseEntity z wynikiem operacji:
     * - success (boolean) - czy operacja się powiodła
     * - courseId (Long) - ID kursu
     * - stats (List<Map>) - lista statystyk dla każdego quizu:
     *   - quizId (Long) - ID quizu
     *   - quizTitle (String) - tytuł quizu
     *   - attempts (Long) - liczba prób
     *   - averageScore (Double) - średni wynik w procentach
     */
    @GetMapping("/{courseId}/quiz-stats")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<?> getCourseQuizStats(@PathVariable Long courseId) {
        Authentication auth = Utils.getAuthentication();
        if (Utils.isTeacher(auth) && courseRepository.findByIdAndTeacherUsername(courseId, Utils.currentUsername()).isEmpty()) {
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "Brak dostępu do tego kursu"));
        }

        List<Quiz> quizzes = quizRepository.findByCourseId(courseId);
        List<Map<String, Object>> stats = quizzes.stream().map(quiz -> {
            List<QuizResult> results = quizResultRepository.findByQuizId(quiz.getId());
            long attempts = results.size();
            double avgScore = results.isEmpty() ? 0.0 :
                    results.stream()
                            .mapToDouble(r -> (r.getCorrectAnswers() * 100.0) / r.getTotalQuestions())
                            .average()
                            .orElse(0.0);

            Map<String, Object> quizStat = new HashMap<>();
            quizStat.put("quizId", quiz.getId());
            quizStat.put("quizTitle", quiz.getTitle());
            quizStat.put("attempts", attempts);
            quizStat.put("averageScore", Math.round(avgScore * 10.0) / 10.0);
            return quizStat;
        }).collect(Collectors.toList());

        log.info("Pobrano statystyki quizów dla kursu ID: {} przez użytkownika {}", courseId, Utils.currentUsername());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "courseId", courseId,
                "stats", stats
        ));
    }
}
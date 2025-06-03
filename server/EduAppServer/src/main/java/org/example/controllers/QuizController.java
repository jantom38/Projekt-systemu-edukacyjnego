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



    @Autowired
    public QuizController(CourseRepository courseRepository,
                          QuizRepository quizRepository,
                          QuizResultRepository quizResultRepository,
                          UserCourseRepository userCourseRepository, QuizQuestionRepository quizQuestionRepository, UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.quizRepository = quizRepository;
        this.quizResultRepository = quizResultRepository;
        this.userCourseRepository = userCourseRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/{id}/quizzes")
    public ResponseEntity<?> getCourseQuizzes(@PathVariable Long id) {
        log.info("Pobieranie quizów dla kursu ID: {}", id);
        Authentication auth = Utils.getAuthentication();
        if (Utils.isTeacher(auth)) {
            boolean owns = courseRepository.findByIdAndTeacherUsername(id, Utils.currentUsername()).isPresent();
            if (!owns) {
                log.warn("Nauczyciel {} próbował uzyskać dostęp do quizów kursu ID: {} bez uprawnień", Utils.currentUsername(), id);
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "message", "Brak dostępu do tego kursu"));
            }
        } else if (Utils.isAdmin(auth)) {
            log.info("Admin {} uzyskuje dostęp do quizów kursu ID: {}", Utils.currentUsername(), id);
        } else {
            Long userId = Utils.getCurrentUserId(userRepository);
            if (!userCourseRepository.existsByUserIdAndCourseId(userId, id)) {
                log.warn("Użytkownik {} nie jest zapisany na kurs ID: {}", userId, id);
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "message", "Nie jesteś zapisany na ten kurs"));
            }
        }

        if (!courseRepository.existsById(id)) {
            log.error("Kurs ID: {} nie znaleziony", id);
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "Kurs nie znaleziony"));
        }

        List<Quiz> quizzes = quizRepository.findByCourseId(id);
        log.info("Pobrano {} quizów dla kursu ID: {}", quizzes.size(), id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "quizzes", quizzes
        ));
    }

    @PostMapping("/{id}/quizzes")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<?> addQuiz(@PathVariable Long id, @RequestBody Quiz quiz) {
        log.info("Próba dodania quizu do kursu ID: {} przez użytkownika {}", id, Utils.currentUsername());
        if (quiz.getTitle() == null || quiz.getTitle().isBlank()) {
            log.warn("Próba dodania quizu z pustym tytułem dla kursu ID: {}", id);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Tytuł quizu jest wymagany"));
        }
        if (quiz.getNumberOfQuestionsToDisplay() <= 0) {
            log.warn("Próba dodania quizu z nieprawidłową ilością pytań do wyświetlenia: {}", quiz.getNumberOfQuestionsToDisplay());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Ilość pytań do wyświetlenia musi być większa niż 0"));
        }

        return courseRepository.findById(id)
                .map(course -> {
                    if (Utils.isTeacher(Utils.getAuthentication()) &&
                            !course.getTeacher().getUsername().equals(Utils.currentUsername())) {
                        log.error("Brak dostępu do kursu ID: {} dla nauczyciela {}", id, Utils.currentUsername());
                        return ResponseEntity.status(403)
                                .body(Map.of("success", false, "message", "Brak dostępu do tego kursu"));
                    }
                    quiz.setCourse(course);
                    Quiz savedQuiz = quizRepository.save(quiz);
                    log.info("Quiz '{}' (ID: {}) dodany do kursu ID: {}", savedQuiz.getTitle(), savedQuiz.getId(), id);
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Quiz dodany pomyślnie",
                            "quiz", savedQuiz
                    ));
                })
                .orElseGet(() -> {
                    log.error("Kurs ID: {} nie znaleziony", id);
                    return ResponseEntity.status(404)
                            .body(Map.of("success", false, "message", "Kurs nie istnieje"));
                });
    }

    @PutMapping("/quizzes/{quizId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<?> updateQuiz(@PathVariable Long quizId, @RequestBody Quiz quiz) {
        log.info("Próba edycji quizu ID: {} przez użytkownika {}", quizId, Utils.currentUsername());
        if (quiz.getTitle() == null || quiz.getTitle().isBlank()) {
            log.warn("Próba edycji quizu ID: {} z pustym tytułem", quizId);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Tytuł quizu jest wymagany"));
        }
        if (quiz.getNumberOfQuestionsToDisplay() <= 0) {
            log.warn("Próba edycji quizu ID: {} z nieprawidłową ilością pytań do wyświetlenia: {}", quizId, quiz.getNumberOfQuestionsToDisplay());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Ilość pytań do wyświetlenia musi być większa niż 0"));
        }

        return quizRepository.findById(quizId)
                .map(existingQuiz -> {
                    Course course = existingQuiz.getCourse();
                    if (Utils.isTeacher(Utils.getAuthentication()) &&
                            !course.getTeacher().getUsername().equals(Utils.currentUsername())) {
                        log.warn("Nauczyciel {} próbował edytować quiz ID: {} bez uprawnień", Utils.currentUsername(), quizId);
                        return ResponseEntity.status(403)
                                .body(Map.of("success", false, "message", "Brak dostępu do tego quizu"));
                    }
                    existingQuiz.setTitle(quiz.getTitle());
                    existingQuiz.setDescription(quiz.getDescription());
                    existingQuiz.setNumberOfQuestionsToDisplay(quiz.getNumberOfQuestionsToDisplay());
                    Quiz updatedQuiz = quizRepository.save(existingQuiz);
                    log.info("Quiz ID: {} edytowany pomyślnie", quizId);
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Quiz zaktualizowany pomyślnie",
                            "quiz", updatedQuiz
                    ));
                })
                .orElseGet(() -> {
                    log.error("Quiz ID: {} nie znaleziony", quizId);
                    return ResponseEntity.status(404)
                            .body(Map.of("success", false, "message", "Quiz nie znaleziony"));
                });
    }

    @DeleteMapping("/quizzes/{quizId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Transactional
    public ResponseEntity<?> deleteQuiz(@PathVariable Long quizId) {
        log.info("Próba usunięcia quizu ID: {} przez użytkownika {}", quizId, Utils.currentUsername());
        return quizRepository.findById(quizId)
                .map(quiz -> {
                    Course course = quiz.getCourse();
                    if (Utils.isTeacher(Utils.getAuthentication()) &&
                            !course.getTeacher().getUsername().equals(Utils.currentUsername())) {
                        log.warn("Nauczyciel {} próbował usunąć quiz ID: {} bez uprawnień", Utils.currentUsername(), quizId);
                        return ResponseEntity.status(403)
                                .body(Map.of("success", false, "message", "Brak dostępu do tego quizu"));
                    }
                    quizQuestionRepository.deleteByQuizId(quizId);
                    quizRepository.delete(quiz);
                    log.info("Quiz ID: {} usunięty pomyślnie", quizId);
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Quiz usunięty pomyślnie"
                    ));
                })
                .orElseGet(() -> {
                    log.error("Quiz ID: {} nie znaleziony", quizId);
                    return ResponseEntity.status(404)
                            .body(Map.of("success", false, "message", "Quiz nie znaleziony"));
                });
    }

    @GetMapping("/{courseId}/quiz-stats")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<?> getCourseQuizStats(@PathVariable Long courseId) {
        log.info("Pobieranie statystyk quizów dla kursu ID: {} przez użytkownika {}", courseId, Utils.currentUsername());

        Authentication auth = Utils.getAuthentication();
        if (Utils.isTeacher(auth) && courseRepository.findByIdAndTeacherUsername(courseId, Utils.currentUsername()).isEmpty()) {
            log.warn("Nauczyciel {} próbował uzyskać dostęp do statystyk kursu ID: {} bez uprawnień", Utils.currentUsername(), courseId);
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

        log.info("Pobrano statystyki dla {} quizów w kursie ID: {}", stats.size(), courseId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "courseId", courseId,
                "stats", stats
        ));
    }
}
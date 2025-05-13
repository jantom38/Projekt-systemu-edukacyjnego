package org.example;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.example.DataBaseRepositories.*;
import org.example.database.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.example.dto.QuizAnswerDTO;
import org.example.dto.QuizQuestionDTO;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/courses")
public class MainControllers {

    @Value("${file.upload-dir:#{systemProperties['user.dir'] + '/uploads'}}")
private String uploadDir;

private final CourseRepository courseRepository;
private final CourseFileRepository courseFileRepository;
private final UserRepository userRepository;
private final QuizRepository quizRepository;
private final UserCourseRepository userCourseRepository;
private final QuizQuestionRepository quizQuestionRepository;
  private final QuizResultRepository quizResultRepository;
private final QuizAnswerRepository quizAnswerRepository;


@Autowired
public MainControllers(CourseRepository courseRepository,
                       CourseFileRepository courseFileRepository,
                       UserRepository userRepository,
                       QuizRepository quizRepository, UserCourseRepository userCourseRepository, QuizQuestionRepository quizQuestionRepository, QuizResultRepository quizResultRepository, QuizAnswerRepository quizAnswerRepository) {
    this.courseRepository = courseRepository;
    this.courseFileRepository = courseFileRepository;
    this.userRepository = userRepository;
    this.quizRepository = quizRepository;

    this.userCourseRepository = userCourseRepository;
    this.quizQuestionRepository = quizQuestionRepository;
    this.quizResultRepository = quizResultRepository;
    this.quizAnswerRepository = quizAnswerRepository;
}

private String currentUsername() {
    return SecurityContextHolder.getContext().getAuthentication().getName();
}

private boolean isTeacher(Authentication auth) {
    return auth.getAuthorities().stream()
            .anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"));
}

@GetMapping
public List<Course> getAllCourses() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (isTeacher(auth)) {
        return courseRepository.findByTeacherUsername(currentUsername());
    }
    return courseRepository.findAll();
}

@PostMapping
@PreAuthorize("hasRole('TEACHER')")
public ResponseEntity<?> addCourse(@RequestBody Course course) {
    if (course.getAccessKey() == null || course.getAccessKey().isBlank()) {
        return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Klucz dostępu jest wymagany"));
    }

    User teacher = userRepository.findByUsername(currentUsername())
            .orElseThrow(() -> new RuntimeException("Zalogowany użytkownik nie istnieje"));

    course.setTeacher(teacher);
    Course savedCourse = courseRepository.save(course);

    return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Course added successfully",
            "course", savedCourse
    ));
}

@PostMapping("/{id}/verify-key")
public ResponseEntity<?> verifyAccessKey(@PathVariable Long id,
                                         @RequestBody Map<String, String> request) {
    String providedKey = request.get("accessKey");
    String username = currentUsername();

    return courseRepository.findById(id)
            .map(course -> {
                if (!course.getAccessKey().equals(providedKey)) {
                    return ResponseEntity.status(403)
                            .body(Map.of("success", false, "message", "Invalid access key"));
                }

                User user = userRepository.findByUsername(username)
                        .orElseThrow(() -> new RuntimeException("User not found"));

                // Sprawdź czy użytkownik już nie jest przypisany do kursu
                if (userCourseRepository.existsByUserIdAndCourseId(user.getId(), id)) {
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "User already enrolled in this course"));
                }

                // Dodaj użytkownika do kursu
                UserCourse userCourse = new UserCourse(user, course);
                userCourseRepository.save(userCourse);

                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Access granted and user enrolled"));
            })
            .orElse(ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "Course not found")));
}

@GetMapping("/{id}/files")
public ResponseEntity<?> getCourseFiles(@PathVariable Long id) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (isTeacher(auth)) {
        boolean owns = courseRepository.findByIdAndTeacherUsername(id, currentUsername()).isPresent();
        if (!owns) {
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "Brak dostępu do tego kursu"));
        }
    }

    if (!courseRepository.existsById(id)) {
        return ResponseEntity.status(404).body(Map.of(
                "success", false,
                "message", "Course not found"));
    }

    return ResponseEntity.ok(courseFileRepository.findByCourseId(id));
}

@DeleteMapping("/{courseId}/files/{fileId}")
@PreAuthorize("hasRole('TEACHER')")
public ResponseEntity<?> deleteCourseFile(@PathVariable Long courseId,
                                          @PathVariable Long fileId) {

    if (courseRepository.findByIdAndTeacherUsername(courseId, currentUsername()).isEmpty()) {
        return ResponseEntity.status(403)
                .body(Map.of("success", false, "message", "Brak dostępu do tego kursu"));
    }

    return courseFileRepository.findById(fileId)
            .filter(cf -> cf.getCourse().getId().equals(courseId))
            .map(cf -> {
                try {
                    String filename = cf.getFileUrl().replaceFirst(".*/files/", "");
                    Path filePath = Paths.get(uploadDir).resolve(filename).toAbsolutePath();
                    Files.deleteIfExists(filePath);
                } catch (IOException e) {
                    log.warn("Błąd podczas usuwania pliku z dysku: " + e.getMessage());
                }

                courseFileRepository.delete(cf);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "File deleted successfully"));
            })
            .orElse(ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "File not found for this course")));
}


@GetMapping("/my-courses")
public ResponseEntity<?> getUserCourses() {
    String username = currentUsername();
    User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));


    List<UserCourse> userCourses = userCourseRepository.findByUserId(user.getId());

    List<Course> courses = userCourses.stream()
            .map(UserCourse::getCourse)
            .collect(Collectors.toList());

    return ResponseEntity.ok(Map.of(
            "success", true,
            "courses", courses
    ));
}
@DeleteMapping("/{id}")
@PreAuthorize("hasRole('TEACHER')")
public ResponseEntity<?> deleteCourse(@PathVariable Long id) {
    return courseRepository.findByIdAndTeacherUsername(id, currentUsername())
            .map(course -> {
                courseRepository.delete(course);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Course deleted successfully"
                ));
            })
            .orElse(ResponseEntity.status(403)
                    .body(Map.of("success", false, "message", "Brak dostępu do tego kursu lub kurs nie istnieje")));
}
/// ENDPOINTY DO QUIZOW
///
///

@GetMapping("/{id}/quizzes")
public ResponseEntity<?> getCourseQuizzes(@PathVariable Long id) {
    log.info("Pobieranie quizów dla kursu ID: {}", id);
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (isTeacher(auth)) {
        boolean owns = courseRepository.findByIdAndTeacherUsername(id, currentUsername()).isPresent();
        if (!owns) {
            log.warn("Nauczyciel {} próbował uzyskać dostęp do quizów kursu ID: {} bez uprawnień", currentUsername(), id);
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "Brak dostępu do tego kursu"));
        }
    }

    if (!courseRepository.existsById(id)) {
        log.error("Kurs ID: {} nie znaleziony", id);
        return ResponseEntity.status(404).body(Map.of(
                "success", false,
                "message", "Course not found"));
    }

    List<Quiz> quizzes = quizRepository.findByCourseId(id);
    log.info("Pobrano {} quizów dla kursu ID: {}", quizzes.size(), id);
    return ResponseEntity.ok(Map.of(
            "success", true,
            "quizzes", quizzes
    ));
}

    @PostMapping("/{id}/quizzes")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> addQuiz(@PathVariable Long id, @RequestBody Quiz quiz) {
        log.info("Próba dodania quizu do kursu ID: {} przez nauczyciela {}", id, currentUsername());
        if (quiz.getTitle() == null || quiz.getTitle().isBlank()) {
            log.warn("Próba dodania quizu z pustym tytułem dla kursu ID: {}", id);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Tytuł quizu jest wymagany"));
        }

        return courseRepository.findByIdAndTeacherUsername(id, currentUsername())
                .map(course -> {
                    quiz.setCourse(course);
                    Quiz savedQuiz = quizRepository.save(quiz);
                    log.info("Quiz '{}' (ID: {}) dodany do kursu ID: {}", savedQuiz.getTitle(), savedQuiz.getId(), id);
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Quiz added successfully",
                            "quiz", savedQuiz
                    ));
                })
                .orElseGet(() -> {
                    log.error("Brak dostępu do kursu ID: {} lub kurs nie istnieje dla nauczyciela {}", id, currentUsername());
                    return ResponseEntity.status(403)
                            .body(Map.of("success", false, "message", "Brak dostępu do tego kursu lub kurs nie istnieje"));
                });
    }

    @PostMapping("/quizzes/{quizId}/questions")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> addQuizQuestion(@PathVariable Long quizId, @RequestBody QuizQuestion question) {
        log.info("Próba dodania pytania do quizu ID: {} przez nauczyciela {}", quizId, currentUsername());
        if (question.getQuestionText() == null || question.getQuestionText().isBlank()) {
            log.warn("Próba dodania pytania z pustą treścią do quizu ID: {}", quizId);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Treść pytania jest wymagana"));
        }
        if (question.getQuestionType() == null ||
                (!question.getQuestionType().equals("multiple_choice") &&
                        !question.getQuestionType().equals("open_ended") &&
                        !question.getQuestionType().equals("true_false"))) {
            log.warn("Nieprawidłowy typ pytania: {}", question.getQuestionType());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Typ pytania musi być 'multiple_choice', 'open_ended' lub 'true_false'"));
        }
        if (question.getQuestionType().equals("multiple_choice")) {
            if (question.getOptions() == null || question.getOptions().size() < 2) {
                log.warn("Za mało opcji dla pytania wielokrotnego wyboru w quizie ID: {}", quizId);
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Pytania wielokrotnego wyboru wymagają co najmniej 2 opcji"));
            }
            if (question.getCorrectAnswer() == null || question.getCorrectAnswer().isBlank()) {
                log.warn("Brak poprawnej odpowiedzi dla pytania wielokrotnego wyboru w quizie ID: {}", quizId);
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Co najmniej jedna poprawna odpowiedź jest wymagana"));
            }
            List<String> correctAnswers = Arrays.asList(question.getCorrectAnswer().split(","));
            for (String correct : correctAnswers) {
                if (!question.getOptions().containsKey(correct)) {
                    log.warn("Nieprawidłowa poprawna odpowiedź '{}' dla pytania wielokrotnego wyboru w quizie ID: {}", correct, quizId);
                    return ResponseEntity.badRequest()
                            .body(Map.of("success", false, "message", "Wszystkie poprawne odpowiedzi muszą być kluczami opcji"));
                }
            }
        } else if (question.getQuestionType().equals("true_false")) {
            if (question.getOptions() == null ||
                    !question.getOptions().entrySet().equals(
                            Set.of(
                                    Map.entry("True", "Prawda"),
                                    Map.entry("False", "Fałsz")
                            )
                    )) {
                log.warn("Nieprawidłowe opcje dla pytania prawda/fałsz w quizie ID: {}", quizId);
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Opcje dla pytania prawda/fałsz muszą być dokładnie 'True: Prawda' i 'False: Fałsz'"));
            }
            if (question.getCorrectAnswer() == null || question.getCorrectAnswer().isBlank()) {
                log.warn("Brak poprawnej odpowiedzi dla pytania prawda/fałsz w quizie ID: {}", quizId);
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Co najmniej jedna poprawna odpowiedź jest wymagana"));
            }
            List<String> correctAnswers = Arrays.asList(question.getCorrectAnswer().split(","));
            for (String correct : correctAnswers) {
                if (!Set.of("True", "False").contains(correct)) {
                    log.warn("Nieprawidłowa poprawna odpowiedź '{}' dla pytania prawda/fałsz w quizie ID: {}", correct, quizId);
                    return ResponseEntity.badRequest()
                            .body(Map.of("success", false, "message", "Poprawne odpowiedzi muszą być 'True' lub 'False'"));
                }
            }
        } else if (question.getQuestionType().equals("open_ended")) {
            if (question.getCorrectAnswer() == null || question.getCorrectAnswer().isBlank()) {
                log.warn("Brak poprawnej odpowiedzi dla pytania otwartego w quizie ID: {}", quizId);
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Poprawna odpowiedź jest wymagana dla pytań otwartych"));
            }
            if (question.getOptions() != null && !question.getOptions().isEmpty()) {
                log.warn("Opcje nie są dozwolone dla pytania otwartego w quizie ID: {}", quizId);
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Pytania otwarte nie mogą zawierać opcji"));
            }
        }

        return quizRepository.findById(quizId)
                .map(quiz -> {
                    Course course = quiz.getCourse();
                    if (!course.getTeacher().getUsername().equals(currentUsername())) {
                        log.warn("Nauczyciel {} próbował dodać pytanie do quizu ID: {} bez uprawnień", currentUsername(), quizId);
                        return ResponseEntity.status(403)
                                .body(Map.of("success", false, "message", "Brak dostępu do tego quizu"));
                    }
                    question.setQuiz(quiz);
                    QuizQuestion savedQuestion = quizQuestionRepository.save(question);
                    log.info("Pytanie '{}' (ID: {}) dodane do quizu ID: {}", savedQuestion.getQuestionText(), savedQuestion.getId(), quizId);
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Question added successfully",
                            "question", savedQuestion
                    ));
                })
                .orElseGet(() -> {
                    log.error("Quiz ID: {} nie znaleziony", quizId);
                    return ResponseEntity.status(404)
                            .body(Map.of("success", false, "message", "Quiz not found"));
                });
    }

    @DeleteMapping("/quizzes/{quizId}")
    @PreAuthorize("hasRole('TEACHER')")
    @Transactional
    public ResponseEntity<?> deleteQuiz(@PathVariable Long quizId) {
        log.info("Próba usunięcia quizu ID: {} przez nauczyciela {}", quizId, currentUsername());
        return quizRepository.findById(quizId)
                .map(quiz -> {
                    Course course = quiz.getCourse();
                    if (!course.getTeacher().getUsername().equals(currentUsername())) {
                        log.warn("Nauczyciel {} próbował usunąć quiz ID: {} bez uprawnień", currentUsername(), quizId);
                        return ResponseEntity.status(403)
                                .body(Map.of("success", false, "message", "Brak dostępu do tego quizu"));
                    }
                    quizQuestionRepository.deleteByQuizId(quizId);
                    quizRepository.delete(quiz);
                    log.info("Quiz ID: {} usunięty pomyślnie", quizId);
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Quiz deleted successfully"
                    ));
                })
                .orElseGet(() -> {
                    log.error("Quiz ID: {} nie znaleziony", quizId);
                    return ResponseEntity.status(404)
                            .body(Map.of("success", false, "message", "Quiz not found"));
                });
    }
// W MainControllers.java

    private boolean validateMultipleChoice(QuizQuestion question, String answer) {
        Set<String> correctAnswers = Set.of(question.getCorrectAnswer().split(","));
        Set<String> userAnswers = Set.of(answer.split(","));
        return correctAnswers.equals(userAnswers);
    }

    private boolean validateTrueFalse(QuizQuestion question, String answer) {
        return question.getCorrectAnswer().equalsIgnoreCase(answer);
    }

    private boolean validateOpenEnded(QuizQuestion question, String answer) {
        return answer.trim().equalsIgnoreCase(question.getCorrectAnswer().trim());
    }
    private User getCurrentUser() {
        return userRepository.findByUsername(currentUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
    private Long getCurrentUserId() {
        String username = currentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("User not found with username: {}", username);
                    return new RuntimeException("User not found");
                })
                .getId();
    }

    private boolean validateAnswer(QuizAnswerDTO answer) {
        try {
            QuizQuestion question = quizQuestionRepository.findById(answer.questionId())
                    .orElseThrow(() -> {
                        log.error("Question not found with ID: {}", answer.questionId());
                        return new RuntimeException("Question not found");
                    });

            boolean isValid = switch (question.getQuestionType()) {
                case "multiple_choice" -> validateMultipleChoice(question, answer.answer());
                case "true_false" -> validateTrueFalse(question, answer.answer());
                case "open_ended" -> validateOpenEnded(question, answer.answer());
                default -> throw new IllegalArgumentException("Unknown question type");
            };

            log.trace("Validation for question {}: {}", answer.questionId(), isValid ? "valid" : "invalid");
            return isValid;
        } catch (Exception e) {
            log.error("Error validating answer for question {}: {}", answer.questionId(), e.getMessage(), e);
            throw e;
        }
    }
    @GetMapping("/{courseId}/available-quizzes")
    public ResponseEntity<?> getAvailableQuizzes(@PathVariable Long courseId) {
        Long userId = getCurrentUserId();
        log.info("Attempting to get available quizzes for course {} by user {}", courseId, userId);

        if (!userCourseRepository.existsByUserIdAndCourseId(userId, courseId)) {
            log.warn("Access denied to course {} for user {}", courseId, userId);
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "Nie jesteś zapisany na ten kurs"));
        }

        List<Quiz> quizzes = quizRepository.findByCourseId(courseId);
        log.info("Successfully retrieved {} quizzes for course {}", quizzes.size(), courseId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "quizzes", quizzes
        ));
    }

    // 2. Pobieranie quizu z pytaniami
    @GetMapping("/quizzes/{quizId}")
    public ResponseEntity<?> getQuizForSolving(@PathVariable Long quizId) {
        log.debug("Fetching quiz with ID {} for solving", quizId);

        return quizRepository.findById(quizId)
                .map(quiz -> {
                    List<QuizQuestionDTO> questions = quizQuestionRepository.findByQuizId(quizId).stream()
                            .map(q -> new QuizQuestionDTO(
                                    q.getId(),
                                    q.getQuestionText(),
                                    q.getQuestionType(),
                                    q.getOptions()
                            ))
                            .collect(Collectors.toList());

                    log.info("Successfully retrieved quiz {} with {} questions", quizId, questions.size());
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "quiz", Map.of(
                                    "id", quiz.getId(),
                                    "title", quiz.getTitle(),
                                    "description", quiz.getDescription(),
                                    "questions", questions
                            )
                    ));
                })
                .orElseGet(() -> {
                    log.warn("Quiz with ID {} not found", quizId);
                    return ResponseEntity.notFound().build();
                });
    }

    // 3. Zapis odpowiedzi i obliczenie wyniku
    @PostMapping("/quizzes/{quizId}/submit")
    public ResponseEntity<?> submitQuizAnswers(
            @PathVariable Long quizId,
            @RequestBody List<QuizAnswerDTO> answers
    ) {
        Long userId = getCurrentUserId();
        log.info("Quiz submission attempt for quiz {} by user {} with {} answers",
                quizId, userId, answers != null ? answers.size() : 0);

        if (answers == null || answers.isEmpty()) {
            log.warn("Empty quiz submission attempt for quiz {} by user {}", quizId, userId);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Brak odpowiedzi"
            ));
        }

        try {
            int totalQuestions = answers.size();
            List<QuizAnswer> quizAnswers = new ArrayList<>();
            int correctAnswers = 0;

            QuizResult result = new QuizResult(
                    getCurrentUser(),
                    quizRepository.getReferenceById(quizId),
                    0, // Ustawimy później
                    totalQuestions,
                    LocalDateTime.now()
            );
            quizResultRepository.save(result);

            for (QuizAnswerDTO answer : answers) {
                QuizQuestion question = quizQuestionRepository.findById(answer.questionId())
                        .orElseThrow(() -> {
                            log.error("Question not found with ID: {}", answer.questionId());
                            return new RuntimeException("Question not found");
                        });
                boolean isCorrect = validateAnswer(answer);

                QuizAnswer quizAnswer = new QuizAnswer();
                quizAnswer.setQuizResult(result);
                quizAnswer.setQuestion(question);
                quizAnswer.setUserAnswer(answer.answer());
                quizAnswer.setIsCorrect(isCorrect);
                quizAnswers.add(quizAnswer);

                if (isCorrect) {
                    correctAnswers++;
                }
            }

            result.setCorrectAnswers(correctAnswers);
            quizResultRepository.save(result);
            quizAnswerRepository.saveAll(quizAnswers);

            log.info("Quiz {} results saved for user {}. Score: {}/{}",
                    quizId, userId, correctAnswers, totalQuestions);

            return ResponseEntity.ok(new SubmissionResultDTO(
                    true,
                    correctAnswers + "/" + totalQuestions,
                    correctAnswers,
                    totalQuestions,
                    (correctAnswers * 100.0) / totalQuestions
            ));
        } catch (Exception e) {
            log.error("Error processing quiz submission for quiz {} by user {}: {}",
                    quizId, userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Wystąpił błąd podczas przetwarzania wyników"
            ));
        }
    }

    // 4. Pobieranie historii wyników
    @GetMapping("/quizzes/{quizId}/results")
    public ResponseEntity<?> getQuizResults(@PathVariable Long quizId) {
        Long userId = getCurrentUserId();
        log.info("Fetching quiz results for quiz {} by user {}", quizId, userId);

        List<QuizResult> results = quizResultRepository.findByUserIdAndQuizId(userId, quizId);
        if (results.isEmpty()) {
            log.warn("No results found for quiz {} and user {}", quizId, userId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "quizId", quizId,
                    "correctAnswers", 0,
                    "totalQuestions", 0,
                    "score", 0.0,
                    "questions", Collections.emptyList()
            ));
        }

        QuizResult latestResult = results.get(0); // Pobieramy najnowszy wynik
        List<QuizAnswer> quizAnswers = quizAnswerRepository.findByQuizResultIdWithQuestions(latestResult.getId());

        List<Map<String, Object>> questionResults = quizAnswers.stream().map(answer -> {
            Map<String, Object> questionResult = new HashMap<>();
            questionResult.put("questionId", answer.getQuestion().getId());
            questionResult.put("questionText", answer.getQuestion().getQuestionText());
            questionResult.put("userAnswer", answer.getUserAnswer());
            questionResult.put("correctAnswer", answer.getQuestion().getCorrectAnswer());
            questionResult.put("isCorrect", answer.isCorrect());
            return questionResult;
        }).collect(Collectors.toList());

        log.debug("Found {} results for quiz {} and user {}", results.size(), quizId, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("quizId", quizId);
        response.put("correctAnswers", latestResult.getCorrectAnswers());
        response.put("totalQuestions", latestResult.getTotalQuestions());
        response.put("score", (latestResult.getCorrectAnswers() * 100.0) / latestResult.getTotalQuestions());
        response.put("questions", questionResults);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{courseId}/quiz-stats")
    public ResponseEntity<?> getCourseQuizStats(@PathVariable Long courseId) {
        log.info("Fetching quiz statistics for course ID: {} by teacher {}", courseId, currentUsername());

        if (courseRepository.findByIdAndTeacherUsername(courseId, currentUsername()).isEmpty()) {
            log.warn("Teacher {} attempted to access stats for course ID: {} without permission", currentUsername(), courseId);
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

        log.info("Retrieved stats for {} quizzes in course ID: {}", stats.size(), courseId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "courseId", courseId,
                "stats", stats
        ));
    }

    // Get detailed results for a specific quiz (all student answers)
    @GetMapping("/quizzes/{quizId}/detailed-results")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> getQuizDetailedResults(@PathVariable Long quizId) {
        log.info("Fetching detailed results for quiz ID: {} by teacher {}", quizId, currentUsername());

        return quizRepository.findById(quizId)
                .map(quiz -> {
                    if (!quiz.getCourse().getTeacher().getUsername().equals(currentUsername())) {
                        log.warn("Teacher {} attempted to access detailed results for quiz ID: {} without permission", currentUsername(), quizId);
                        return ResponseEntity.status(403).body(Map.of(
                                "success", false,
                                "message", "Brak dostępu do tego quizu"));
                    }

                    List<QuizResult> results = quizResultRepository.findByQuizId(quizId);
                    List<Map<String, Object>> detailedResults = results.stream().map(result -> {
                        List<QuizAnswer> answers = quizAnswerRepository.findByQuizResultIdWithQuestions(result.getId());
                        Map<String, Object> studentResult = new HashMap<>();
                        studentResult.put("userId", result.getUser().getId());
                        studentResult.put("username", result.getUser().getUsername());
                        studentResult.put("correctAnswers", result.getCorrectAnswers());
                        studentResult.put("totalQuestions", result.getTotalQuestions());
                        studentResult.put("score", (result.getCorrectAnswers() * 100.0) / result.getTotalQuestions());
                        studentResult.put("completionDate", result.getCompletionDate());

                        List<Map<String, Object>> questionAnswers = answers.stream().map(answer -> {
                            Map<String, Object> answerDetail = new HashMap<>();
                            answerDetail.put("questionId", answer.getQuestion().getId());
                            answerDetail.put("questionText", answer.getQuestion().getQuestionText());
                            answerDetail.put("userAnswer", answer.getUserAnswer());
                            answerDetail.put("correctAnswer", answer.getQuestion().getCorrectAnswer());
                            answerDetail.put("isCorrect", answer.isCorrect());
                            return answerDetail;
                        }).collect(Collectors.toList());

                        studentResult.put("answers", questionAnswers);
                        return studentResult;
                    }).collect(Collectors.toList());

                    log.info("Retrieved detailed results for quiz ID: {} with {} student submissions", quizId, detailedResults.size());
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "quizId", quizId,
                            "quizTitle", quiz.getTitle(),
                            "results", detailedResults
                    ));
                })
                .orElseGet(() -> {
                    log.error("Quiz ID: {} not found", quizId);
                    return ResponseEntity.status(404).body(Map.of(
                            "success", false,
                            "message", "Quiz not found"));
                });
    }
    @GetMapping("/{courseId}/users")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> getCourseUsers(@PathVariable Long courseId) {
        log.info("Pobieranie użytkowników kursu ID: {} przez nauczyciela {}", courseId, currentUsername());

        // Sprawdź czy nauczyciel ma dostęp do kursu
        Course course = courseRepository.findByIdAndTeacherUsername(courseId, currentUsername())
                .orElseThrow(() -> {
                    log.warn("Brak dostępu do kursu ID: {} dla nauczyciela {}", courseId, currentUsername());
                    return new RuntimeException("Brak uprawnień lub kurs nie istnieje");
                });

        // Pobierz wszystkich użytkowników kursu
        List<UserCourse> userCourses = userCourseRepository.findByCourseId(courseId);

        // Mapowanie do formatu odpowiedzi
        List<Map<String, Object>> users = userCourses.stream()
                .map(uc -> {
                    User user = uc.getUser();
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", user.getId());
                    userMap.put("username", user.getUsername());
                    userMap.put("role", user.getRole().name());
                    userMap.put("joinedAt", uc.getJoinedAt());
                    return userMap;
                })
                .collect(Collectors.toList());


        return ResponseEntity.ok(Map.of(
                "success", true,
                "users", users
        ));
    }

    @DeleteMapping("/{courseId}/users/{userId}")
    @PreAuthorize("hasRole('TEACHER')")
    @Transactional
    public ResponseEntity<?> removeUserFromCourse(@PathVariable Long courseId,
                                                  @PathVariable Long userId) {
        log.info("Próba usunięcia użytkownika ID: {} z kursu ID: {} przez {}", userId, courseId, currentUsername());

        // Sprawdź czy nauczyciel ma dostęp do kursu
        courseRepository.findByIdAndTeacherUsername(courseId, currentUsername())
                .orElseThrow(() -> {
                    log.warn("Brak dostępu do kursu ID: {} dla nauczyciela {}", courseId, currentUsername());
                    return new RuntimeException("Brak uprawnień lub kurs nie istnieje");
                });

        // Znajdź i usuń przypisanie użytkownika do kursu
        userCourseRepository.findByUserIdAndCourseId(userId, courseId)
                .ifPresentOrElse(
                        userCourse -> {
                            userCourseRepository.delete(userCourse);
                            log.info("Usunięto użytkownika ID: {} z kursu ID: {}", userId, courseId);
                        },
                        () -> {
                            log.warn("Użytkownik ID: {} nie jest przypisany do kursu ID: {}", userId, courseId);
                            throw new RuntimeException("Użytkownik nie jest przypisany do tego kursu");
                        }
                );

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Użytkownik został usunięty z kursu"
        ));
    }
}

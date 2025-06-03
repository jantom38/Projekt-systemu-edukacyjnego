package org.example;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.example.DataBaseRepositories.*;
import org.example.controllers.SubmissionResultDTO;
import org.example.database.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.example.dto.QuizAnswerDTO;
import org.example.dto.QuizQuestionDTO;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
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
    private final RoleCodeRepository roleCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    public MainControllers(CourseRepository courseRepository,
                           CourseFileRepository courseFileRepository,
                           UserRepository userRepository,
                           QuizRepository quizRepository,
                           UserCourseRepository userCourseRepository,
                           QuizQuestionRepository quizQuestionRepository,
                           QuizResultRepository quizResultRepository,
                           QuizAnswerRepository quizAnswerRepository,
                           RoleCodeRepository roleCodeRepository,
                           PasswordEncoder passwordEncoder) {
        this.courseRepository = courseRepository;
        this.courseFileRepository = courseFileRepository;
        this.userRepository = userRepository;
        this.quizRepository = quizRepository;
        this.userCourseRepository = userCourseRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.quizResultRepository = quizResultRepository;
        this.quizAnswerRepository = quizAnswerRepository;
        this.roleCodeRepository = roleCodeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private String generateRandomCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            code.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return code.toString();
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private boolean isTeacher(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_TEACHER"));
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @GetMapping
    public List<Course> getAllCourses() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (isTeacher(auth)) {
            return courseRepository.findByTeacherUsername(currentUsername());
        } else if (isAdmin(auth)) {
            return courseRepository.findAll();
        }
        return courseRepository.findAll();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
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
                "message", "Kurs dodany pomyślnie",
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
                                .body(Map.of("success", false, "message", "Nieprawidłowy klucz dostępu"));
                    }

                    User user = userRepository.findByUsername(username)
                            .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));

                    if (userCourseRepository.existsByUserIdAndCourseId(user.getId(), id)) {
                        return ResponseEntity.ok(Map.of(
                                "success", true,
                                "message", "Użytkownik jest już zapisany na ten kurs"));
                    }

                    UserCourse userCourse = new UserCourse(user, course);
                    userCourseRepository.save(userCourse);

                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Dostęp przyznany i użytkownik zapisany"));
                })
                .orElse(ResponseEntity.status(404)
                        .body(Map.of("success", false, "message", "Kurs nie znaleziony")));
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
        } else if (isAdmin(auth)) {
            log.info("Admin {} uzyskuje dostęp do plików kursu ID: {}", currentUsername(), id);
        } else {
            Long userId = getCurrentUserId();
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

        return ResponseEntity.ok(courseFileRepository.findByCourseId(id));
    }

    @DeleteMapping("/{courseId}/files/{fileId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<?> deleteCourseFile(@PathVariable Long courseId,
                                              @PathVariable Long fileId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (isTeacher(auth) && courseRepository.findByIdAndTeacherUsername(courseId, currentUsername()).isEmpty()) {
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
                            "message", "Plik usunięty pomyślnie"));
                })
                .orElse(ResponseEntity.status(404)
                        .body(Map.of(" success", false, "message", "Plik nie znaleziony dla tego kursu")));
    }

    @GetMapping("/my-courses")
    public ResponseEntity<?> getUserCourses() {
        String username = currentUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));

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
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<?> deleteCourse(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (isTeacher(auth) && courseRepository.findByIdAndTeacherUsername(id, currentUsername()).isEmpty()) {
            return ResponseEntity.status(403)
                    .body(Map.of("success", false, "message", "Brak dostępu do tego kursu lub kurs nie istnieje"));
        }

        return courseRepository.findById(id)
                .map(course -> {
                    courseRepository.delete(course);
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Kurs usunięty pomyślnie"
                    ));
                })
                .orElse(ResponseEntity.status(404)
                        .body(Map.of("success", false, "message", "Kurs nie istnieje")));
    }

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
        } else if (isAdmin(auth)) {
            log.info("Admin {} uzyskuje dostęp do quizów kursu ID: {}", currentUsername(), id);
        } else {
            Long userId = getCurrentUserId();
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
        log.info("Próba dodania quizu do kursu ID: {} przez użytkownika {}", id, currentUsername());
        if (quiz.getTitle() == null || quiz.getTitle().isBlank()) {
            log.warn("Próba dodania quizu z pustym tytułem dla kursu ID: {}", id);
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Tytuł quizu jest wymagany"));
        }
        // Validate numberOfQuestionsToDisplay
        if (quiz.getNumberOfQuestionsToDisplay() <= 0) {
            log.warn("Próba dodania quizu z nieprawidłową ilością pytań do wyświetlenia: {}", quiz.getNumberOfQuestionsToDisplay());
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Ilość pytań do wyświetlenia musi być większa niż 0"));
        }

        return courseRepository.findById(id)
                .map(course -> {
                    if (isTeacher(SecurityContextHolder.getContext().getAuthentication()) &&
                            !course.getTeacher().getUsername().equals(currentUsername())) {
                        log.error("Brak dostępu do kursu ID: {} dla nauczyciela {}", id, currentUsername());
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

    @PostMapping("/quizzes/{quizId}/questions")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<?> addQuizQuestion(@PathVariable Long quizId, @RequestBody QuizQuestion question) {
        log.info("Próba dodania pytania do quizu ID: {} przez użytkownika {}", quizId, currentUsername());
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
                    if (isTeacher(SecurityContextHolder.getContext().getAuthentication()) &&
                            !course.getTeacher().getUsername().equals(currentUsername())) {
                        log.warn("Nauczyciel {} próbował dodać pytanie do quizu ID: {} bez uprawnień", currentUsername(), quizId);
                        return ResponseEntity.status(403)
                                .body(Map.of("success", false, "message", "Brak dostępu do tego quizu"));
                    }
                    question.setQuiz(quiz);
                    QuizQuestion savedQuestion = quizQuestionRepository.save(question);
                    log.info("Pytanie '{}' (ID: {}) dodane do quizu ID: {}", savedQuestion.getQuestionText(), savedQuestion.getId(), quizId);
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Pytanie dodane pomyślnie",
                            "question", savedQuestion
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
        log.info("Próba usunięcia quizu ID: {} przez użytkownika {}", quizId, currentUsername());
        return quizRepository.findById(quizId)
                .map(quiz -> {
                    Course course = quiz.getCourse();
                    if (isTeacher(SecurityContextHolder.getContext().getAuthentication()) &&
                            !course.getTeacher().getUsername().equals(currentUsername())) {
                        log.warn("Nauczyciel {} próbował usunąć quiz ID: {} bez uprawnień", currentUsername(), quizId);
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
                .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));
    }

    private Long getCurrentUserId() {
        String username = currentUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("Użytkownik nie znaleziony z nazwą: {}", username);
                    return new RuntimeException("Użytkownik nie znaleziony");
                })
                .getId();
    }

    private boolean validateAnswer(QuizAnswerDTO answer) {
        try {
            QuizQuestion question = quizQuestionRepository.findById(answer.questionId())
                    .orElseThrow(() -> {
                        log.error("Pytanie nie znalezione z ID: {}", answer.questionId());
                        return new RuntimeException("Pytanie nie znalezione");
                    });

            boolean isValid = switch (question.getQuestionType()) {
                case "multiple_choice" -> validateMultipleChoice(question, answer.answer());
                case "true_false" -> validateTrueFalse(question, answer.answer());
                case "open_ended" -> validateOpenEnded(question, answer.answer());
                default -> throw new IllegalArgumentException("Nieznany typ pytania");
            };

            log.trace("Walidacja dla pytania {}: {}", answer.questionId(), isValid ? "poprawna" : "niepoprawna");
            return isValid;
        } catch (Exception e) {
            log.error("Błąd podczas walidacji odpowiedzi dla pytania {}: {}", answer.questionId(), e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/{courseId}/available-quizzes")
    public ResponseEntity<?> getAvailableQuizzes(@PathVariable Long courseId) {
        Long userId = getCurrentUserId();
        log.info("Próba pobrania dostępnych quizów dla kursu {} przez użytkownika {}", courseId, userId);

        if (!userCourseRepository.existsByUserIdAndCourseId(userId, courseId)) {
            log.warn("Brak dostępu do kursu {} dla użytkownika {}", courseId, userId);
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "Nie jesteś zapisany na ten kurs"));
        }

        List<Quiz> quizzes = quizRepository.findByCourseId(courseId);
        log.info("Pobrano {} quizów dla kursu {}", quizzes.size(), courseId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "quizzes", quizzes
        ));
    }

    @GetMapping("/quizzes/{quizId}")
    public ResponseEntity<?> getQuizForSolving(@PathVariable Long quizId) {

        log.debug("Pobieranie quizu z ID {} do rozwiązania", quizId);

        return quizRepository.findById(quizId)
                .map(quiz -> {
                    List<QuizQuestion> allQuestions = quizQuestionRepository.findByQuizId(quizId);
                    Collections.shuffle(allQuestions); // Shuffle all questions
                    int questionsToDisplay = Math.min(quiz.getNumberOfQuestionsToDisplay(), allQuestions.size()); // Get the minimum of requested and available questions
                    List<QuizQuestionDTO> selectedQuestions = allQuestions.stream()
                            .limit(questionsToDisplay) // Limit to the desired number of questions
                            .map(q -> new QuizQuestionDTO(
                                    q.getId(),
                                    q.getQuestionText(),
                                    q.getQuestionType(),
                                    q.getOptions()
                            ))
                            .collect(Collectors.toList());

                    log.info("Pobrano quiz {} z {} (wybrano {}) pytaniami", quizId, allQuestions.size(), selectedQuestions.size());
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "quiz", Map.of(
                                    "id", quiz.getId(),
                                    "title", quiz.getTitle(),
                                    "description", quiz.getDescription(),
                                    "questions", selectedQuestions // Return the selected questions
                            )
                    ));
                })
                .orElseGet(() -> {
                    log.warn("Quiz z ID {} nie znaleziony", quizId);
                    return ResponseEntity.notFound().build();
                });
    }

    @PostMapping("/quizzes/{quizId}/submit")
    public ResponseEntity<?> submitQuizAnswers(
            @PathVariable Long quizId,
            @RequestBody List<QuizAnswerDTO> answers
    ) {
        Long userId = getCurrentUserId();
        log.info("Próba przesłania quizu {} przez użytkownika {} z {} odpowiedziami",
                quizId, userId, answers != null ? answers.size() : 0);

        if (answers == null || answers.isEmpty()) {
            log.warn("Pusta próba przesłania quizu {} przez użytkownika {}", quizId, userId);
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
                    0,
                    totalQuestions,
                    LocalDateTime.now()
            );
            quizResultRepository.save(result);

            for (QuizAnswerDTO answer : answers) {
                QuizQuestion question = quizQuestionRepository.findById(answer.questionId())
                        .orElseThrow(() -> {
                            log.error("Pytanie nie znalezione z ID: {}", answer.questionId());
                            return new RuntimeException("Pytanie nie znalezione");
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

            log.info("Wyniki quizu {} zapisane dla użytkownika {}. Wynik: {}/{}",
                    quizId, userId, correctAnswers, totalQuestions);

            return ResponseEntity.ok(new SubmissionResultDTO(
                    true,
                    correctAnswers + "/" + totalQuestions,
                    correctAnswers,
                    totalQuestions,
                    (correctAnswers * 100.0) / totalQuestions
            ));
        } catch (Exception e) {
            log.error("Błąd podczas przetwarzania przesłania quizu {} przez użytkownika {}: {}",
                    quizId, userId, e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Wystąpił błąd podczas przetwarzania wyników"
            ));
        }
    }

    @GetMapping("/quizzes/{quizId}/results")
    public ResponseEntity<?> getQuizResults(@PathVariable Long quizId) {
        Long userId = getCurrentUserId();
        log.info("Pobieranie wyników quizu {} dla użytkownika {}", quizId, userId);

        List<QuizResult> results = quizResultRepository.findByUserIdAndQuizId(userId, quizId);
        if (results.isEmpty()) {
            log.warn("Brak wyników dla quizu {} i użytkownika {}", quizId, userId);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "quizId", quizId,
                    "correctAnswers", 0,
                    "totalQuestions", 0,
                    "score", 0.0,
                    "questions", Collections.emptyList()
            ));
        }

        QuizResult latestResult = results.get(0);
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

        log.debug("Znaleziono {} wyników dla quizu {} i użytkownika {}", results.size(), quizId, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("quizId", quizId);
        response.put("correctAnswers", latestResult.getCorrectAnswers());
        response.put("totalQuestions", latestResult.getTotalQuestions());
        response.put("score", (latestResult.getCorrectAnswers() * 100.0) / latestResult.getTotalQuestions());
        response.put("questions", questionResults);

        return ResponseEntity.ok(response);
    }
    @PutMapping("/quizzes/{quizId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<?> updateQuiz(@PathVariable Long quizId, @RequestBody Quiz quiz) {
        log.info("Próba edycji quizu ID: {} przez użytkownika {}", quizId, currentUsername());
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
                    if (isTeacher(SecurityContextHolder.getContext().getAuthentication()) &&
                            !course.getTeacher().getUsername().equals(currentUsername())) {
                        log.warn("Nauczyciel {} próbował edytować quiz ID: {} bez uprawnień", currentUsername(), quizId);
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

    @PutMapping("/quizzes/{quizId}/questions/{questionId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<?> updateQuizQuestion(@PathVariable Long quizId, @PathVariable Long questionId, @RequestBody QuizQuestion question) {
        log.info("Próba edycji pytania ID: {} w quizie ID: {} przez użytkownika {}", questionId, quizId, currentUsername());
        if (question.getQuestionText() == null || question.getQuestionText().isBlank()) {
            log.warn("Próba edycji pytania ID: {} z pustą treścią", questionId);
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
                log.warn("Za mało opcji dla pytania wielokrotnego wyboru ID: {}", questionId);
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Pytania wielokrotnego wyboru wymagają co najmniej 2 opcji"));
            }
            if (question.getCorrectAnswer() == null || question.getCorrectAnswer().isBlank()) {
                log.warn("Brak poprawnej odpowiedzi dla pytania wielokrotnego wyboru ID: {}", questionId);
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Co najmniej jedna poprawna odpowiedź jest wymagana"));
            }
            List<String> correctAnswers = Arrays.asList(question.getCorrectAnswer().split(","));
            for (String correct : correctAnswers) {
                if (!question.getOptions().containsKey(correct)) {
                    log.warn("Nieprawidłowa poprawna odpowiedź '{}' dla pytania ID: {}", correct, questionId);
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
                log.warn("Nieprawidłowe opcje dla pytania prawda/fałsz ID: {}", questionId);
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Opcje dla pytania prawda/fałsz muszą być dokładnie 'True: Prawda' i 'False: Fałsz'"));
            }
            if (question.getCorrectAnswer() == null || question.getCorrectAnswer().isBlank()) {
                log.warn("Brak poprawnej odpowiedzi dla pytania prawda/fałsz ID: {}", questionId);
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Co najmniej jedna poprawna odpowiedź jest wymagana"));
            }
            List<String> correctAnswers = Arrays.asList(question.getCorrectAnswer().split(","));
            for (String correct : correctAnswers) {
                if (!Set.of("True", "False").contains(correct)) {
                    log.warn("Nieprawidłowa poprawna odpowiedź '{}' dla pytania prawda/fałsz ID: {}", correct, questionId);
                    return ResponseEntity.badRequest()
                            .body(Map.of("success", false, "message", "Poprawne odpowiedzi muszą być 'True' lub 'False'"));
                }
            }
        } else if (question.getQuestionType().equals("open_ended")) {
            if (question.getCorrectAnswer() == null || question.getCorrectAnswer().isBlank()) {
                log.warn("Brak poprawnej odpowiedzi dla pytania otwartego ID: {}", questionId);
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Poprawna odpowiedź jest wymagana dla pytań otwartych"));
            }
            if (question.getOptions() != null && !question.getOptions().isEmpty()) {
                log.warn("Opcje nie są dozwolone dla pytania otwartego ID: {}", questionId);
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Pytania otwarte nie mogą zawierać opcji"));
            }
        }

        return quizQuestionRepository.findById(questionId)
                .map(existingQuestion -> {
                    if (!existingQuestion.getQuiz().getId().equals(quizId)) {
                        log.warn("Pytanie ID: {} nie należy do quizu ID: {}", questionId, quizId);
                        return ResponseEntity.badRequest()
                                .body(Map.of("success", false, "message", "Pytanie nie należy do tego quizu"));
                    }
                    Course course = existingQuestion.getQuiz().getCourse();
                    if (isTeacher(SecurityContextHolder.getContext().getAuthentication()) &&
                            !course.getTeacher().getUsername().equals(currentUsername())) {
                        log.warn("Nauczyciel {} próbował edytować pytanie ID: {} bez uprawnień", currentUsername(), questionId);
                        return ResponseEntity.status(403)
                                .body(Map.of("success", false, "message", "Brak dostępu do tego quizu"));
                    }
                    existingQuestion.setQuestionText(question.getQuestionText());
                    existingQuestion.setQuestionType(question.getQuestionType());
                    existingQuestion.setOptions(question.getOptions());
                    existingQuestion.setCorrectAnswer(question.getCorrectAnswer());
                    QuizQuestion updatedQuestion = quizQuestionRepository.save(existingQuestion);
                    log.info("Pytanie ID: {} w quizie ID: {} zaktualizowane pomyślnie", questionId, quizId);
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Pytanie zaktualizowane pomyślnie",
                            "question", updatedQuestion
                    ));
                })
                .orElseGet(() -> {
                    log.error("Pytanie ID: {} nie znaleziony", questionId);
                    return ResponseEntity.status(404)
                            .body(Map.of("success", false, "message", "Pytanie nie znalezione"));
                });
    }

    @DeleteMapping("/quizzes/{quizId}/questions/{questionId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Transactional
    public ResponseEntity<?> deleteQuizQuestion(@PathVariable Long quizId, @PathVariable Long questionId) {
        log.info("Próba usunięcia pytania ID: {} z quizu ID: {} przez użytkownika {}", questionId, quizId, currentUsername());
        return quizQuestionRepository.findById(questionId)
                .map(question -> {
                    if (!question.getQuiz().getId().equals(quizId)) {
                        log.warn("Pytanie ID: {} nie należy do quizu ID: {}", questionId, quizId);
                        return ResponseEntity.badRequest()
                                .body(Map.of("success", false, "message", "Pytanie nie należy do tego quizu"));
                    }
                    Course course = question.getQuiz().getCourse();
                    if (isTeacher(SecurityContextHolder.getContext().getAuthentication()) &&
                            !course.getTeacher().getUsername().equals(currentUsername())) {
                        log.warn("Nauczyciel {} próbował usunąć pytanie ID: {} bez uprawnień", currentUsername(), questionId);
                        return ResponseEntity.status(403)
                                .body(Map.of("success", false, "message", "Brak dostępu do tego quizu"));
                    }
                    quizQuestionRepository.delete(question);
                    log.info("Pytanie ID: {} usunięte pomyślnie", questionId);
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Pytanie usunięte pomyślnie"
                    ));
                })
                .orElseGet(() -> {
                    log.error("Pytanie ID: {} nie znaleziono", questionId);
                    return ResponseEntity.status(404)
                            .body(Map.of("success", false, "message", "Pytanie nie znaleziono"));
                });
    }

    // ZEDYTOWANY ENDPOINT getQuizForEdit - na wzór getQuizForSolving
    @GetMapping("/quizzes/{quizId}/edit")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<?> getQuizForEdit(@PathVariable Long quizId) {
        String username = currentUsername();
        log.info("Próba pobrania quizu ID: {} do edycji przez użytkownika {}", quizId, username);

        Optional<Quiz> quizOptional = quizRepository.findById(quizId);

        if (quizOptional.isEmpty()) {
            log.error("Quiz ID: {} nie znaleziony", quizId);
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "Quiz nie znaleziony"));
        }

        Quiz quiz = quizOptional.get();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Sprawdzenie autoryzacji dla nauczyciela
        if (isTeacher(auth)) {
            Course course = quiz.getCourse();
            // Jeśli quiz nie ma kursu LUB kurs nie ma nauczyciela LUB nazwa nauczyciela kursu nie pasuje do aktualnego użytkownika
            if (course == null || course.getTeacher() == null || !course.getTeacher().getUsername().equals(username)) {
                log.warn("Nauczyciel {} próbował pobrać quiz ID: {} bez uprawnień (quiz nie należy do jego kursu)", username, quizId);
                return ResponseEntity.status(403)
                        .body(Map.of("success", false, "message", "Brak dostępu do edycji tego quizu. Quiz nie należy do Twojego kursu."));
            }
        }
        // Jeśli użytkownik jest ADMINEM, ma dostęp automatycznie dzięki @PreAuthorize

        // Pobierz wszystkie pytania quizu i przekształć je na List<Map<String, Object>>
        List<QuizQuestion> allQuestions = quizQuestionRepository.findByQuizId(quizId);
        List<Map<String, Object>> questionMaps = allQuestions.stream()
                .map(q -> Map.of( // Tworzymy mapę dla każdego pytania
                        "questionId", q.getId(),
                        "questionText", q.getQuestionText(),
                        "questionType", q.getQuestionType(),
                        "options", q.getOptions() != null ? q.getOptions() : Map.of(), // Zwróć mapę opcji, jeśli istnieje, inaczej pustą mapę
                        "correctAnswer", q.getCorrectAnswer() != null ? q.getCorrectAnswer() : "" // Zwróć poprawną odpowiedź
                ))
                .collect(Collectors.toList());

        log.info("Quiz ID: {} z wszystkimi pytaniami pobrany pomyślnie do edycji dla {}", quizId, username);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "quiz", Map.of( // Tworzymy mapę dla quizu, jak w getQuizForSolving
                        "id", quiz.getId(),
                        "title", quiz.getTitle(),
                        "description", quiz.getDescription(),
                        "numberOfQuestionsToDisplay", quiz.getNumberOfQuestionsToDisplay(), // Dodaj to pole, jeśli chcesz je edytować
                        "questions", questionMaps // Zwracamy listę map pytań
                )
        ));
    }

    @GetMapping("/{courseId}/quiz-stats")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<?> getCourseQuizStats(@PathVariable Long courseId) {
        log.info("Pobieranie statystyk quizów dla kursu ID: {} przez użytkownika {}", courseId, currentUsername());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (isTeacher(auth) && courseRepository.findByIdAndTeacherUsername(courseId, currentUsername()).isEmpty()) {
            log.warn("Nauczyciel {} próbował uzyskać dostęp do statystyk kursu ID: {} bez uprawnień", currentUsername(), courseId);
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

    @GetMapping("/quizzes/{quizId}/detailed-results")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<?> getQuizDetailedResults(@PathVariable Long quizId) {
        log.info("Pobieranie szczegółowych wyników dla quizu ID: {} przez użytkownika {}", quizId, currentUsername());

        return quizRepository.findById(quizId)
                .map(quiz -> {
                    if (isTeacher(SecurityContextHolder.getContext().getAuthentication()) &&
                            !quiz.getCourse().getTeacher().getUsername().equals(currentUsername())) {
                        log.warn("Nauczyciel {} próbował uzyskać dostęp do szczegółowych wyników quizu ID: {} bez uprawnień", currentUsername(), quizId);
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

                    log.info("Pobrano szczegółowe wyniki dla quizu ID: {} z {} zgłoszeniami studentów", quizId, detailedResults.size());
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "quizId", quizId,
                            "quizTitle", quiz.getTitle(),
                            "results", detailedResults
                    ));
                })
                .orElseGet(() -> {
                    log.error("Quiz ID: {} nie znaleziony", quizId);
                    return ResponseEntity.status(404).body(Map.of(
                            "success", false,
                            "message", "Quiz nie znaleziony"));
                });
    }

    @GetMapping("/{courseId}/users")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<?> getCourseUsers(@PathVariable Long courseId) {
        log.info("Pobieranie użytkowników kursu ID: {} przez użytkownika {}", courseId, currentUsername());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (isTeacher(auth) && courseRepository.findByIdAndTeacherUsername(courseId, currentUsername()).isEmpty()) {
            log.warn("Brak dostępu do kursu ID: {} dla nauczyciela {}", courseId, currentUsername());
            return ResponseEntity.status(403)
                    .body(Map.of("success", false, "message", "Brak uprawnień lub kurs nie istnieje"));
        }

        List<UserCourse> userCourses = userCourseRepository.findByCourseId(courseId);

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
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Transactional
    public ResponseEntity<?> removeUserFromCourse(@PathVariable Long courseId,
                                                  @PathVariable Long userId) {
        log.info("Próba usunięcia użytkownika ID: {} z kursu ID: {} przez {}", userId, courseId, currentUsername());

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (isTeacher(auth) && courseRepository.findByIdAndTeacherUsername(courseId, currentUsername()).isEmpty()) {
            log.warn("Brak dostępu do kursu ID: {} dla nauczyciela {}", courseId, currentUsername());
            return ResponseEntity.status(403)
                    .body(Map.of("success", false, "message", "Brak uprawnień lub kurs nie istnieje"));
        }

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

    @PostMapping("/auth/generate-student-code")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> generateStudentCode(@RequestBody Map<String, String> request) {
        log.info("Próba wygenerowania kodu dla roli STUDENT przez nauczyciela {}", currentUsername());

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

        User teacher = userRepository.findByUsername(currentUsername())
                .orElseThrow(() -> new RuntimeException("Nauczyciel nie znaleziony"));

        String code;
        int attempts = 0;
        final int maxAttempts = 10;
        do {
            code = generateRandomCode();
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
        log.info("Pobieranie listy wszystkich użytkowników przez admina {}", currentUsername());
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
        log.info("Próba nadania roli TEACHER użytkownikowi ID: {} przez admina {}", userId, currentUsername());
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
        log.info("Próba zmiany roli na STUDENT dla użytkownika ID: {} przez admina {}", userId, currentUsername());
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
        String adminUsername = currentUsername();
        log.info("Administrator {} próbuje usunąć użytkownika ID: {}", adminUsername, userId);

        // 1. Fetch the user to be deleted
        User userToDelete = userRepository.findById(userId)
                .orElse(null);

        if (userToDelete == null) {
            log.warn("Użytkownik ID: {} nie znaleziony do usunięcia.", userId);
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "Użytkownik nie znaleziony"));
        }

        // 2. Prevent admin from deleting themselves or another admin
        User adminUser = userRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono aktualnie zalogowanego administratora")); // Should not happen

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

        // 3. Delete UserCourse associations
        // Assuming UserCourseRepository has a method like findByUserId
        List<UserCourse> userCourses = userCourseRepository.findByUserId(userId);
        if (!userCourses.isEmpty()) {
            userCourseRepository.deleteAllInBatch(userCourses); // Efficient for bulk delete
            log.info("Usunięto {} przypisań do kursów (UserCourse) dla użytkownika ID: {}", userCourses.size(), userId);
        }

        // 4. Delete QuizAnswers related to the user's QuizResults
        // User entity has @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true) for quizResults.
        // This means when a User is deleted, their QuizResults are automatically deleted.
        // However, QuizResult does NOT cascade delete QuizAnswers. So, QuizAnswers must be deleted manually first.

        // Assuming QuizResultRepository has a method findByUserId(Long userId)
        List<QuizResult> quizResultsForUser = quizResultRepository.findByUserId(userId);
        if (quizResultsForUser != null && !quizResultsForUser.isEmpty()) {
            log.info("Znaleziono {} wyników quizów (QuizResult) dla użytkownika ID: {}. Rozpoczynanie usuwania powiązanych odpowiedzi (QuizAnswer)...", quizResultsForUser.size(), userId);
            for (QuizResult result : quizResultsForUser) {
                // Assuming QuizAnswerRepository has a method deleteByQuizResultId(Long quizResultId)
                quizAnswerRepository.deleteByQuizResultId(result.getId());
                log.debug("Usunięto odpowiedzi (QuizAnswer) dla QuizResult ID: {}", result.getId());
            }
            log.info("Zakończono usuwanie odpowiedzi (QuizAnswer) dla wyników quizów użytkownika ID: {}", userId);
        } else {
            log.info("Nie znaleziono wyników quizów (QuizResult) dla użytkownika ID: {} do przetworzenia.", userId);
        }

        // Note: RoleCode entities generated by a teacher are not directly linked to all students who used them,
        // but to the teacher who generated them. If a student is deleted, their used RoleCode (which is already inactive)
        // doesn't need specific handling here beyond the student's own deletion.

        // 5. Delete the user
        // Deleting the user will also cascade to delete their QuizResults due to CascadeType.ALL on User.quizResults
        userRepository.delete(userToDelete);
        log.info("Użytkownik ID: {} został pomyślnie usunięty przez administratora {}", userId, adminUsername);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Użytkownik i wszystkie jego powiązane dane zostały usunięte pomyślnie."
        ));
    }
}
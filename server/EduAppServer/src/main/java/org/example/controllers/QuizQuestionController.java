package org.example.controllers;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.example.DataBaseRepositories.*;
import org.example.database.*;
import org.example.dto.QuizQuestionDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/courses")
public class QuizQuestionController {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final UserRepository userRepository;
    private final UserCourseRepository userCourseRepository;

    @Autowired
    public QuizQuestionController(QuizRepository quizRepository,
                                  QuizQuestionRepository quizQuestionRepository,
                                  UserRepository userRepository,
                                  UserCourseRepository userCourseRepository) {
        this.quizRepository = quizRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.userRepository = userRepository;
        this.userCourseRepository = userCourseRepository;
    }

    @PostMapping("/quizzes/{quizId}/questions")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<?> addQuizQuestion(@PathVariable Long quizId, @RequestBody QuizQuestion question) {
        log.info("Próba dodania pytania do quizu ID: {} przez użytkownika {}", quizId, Utils.currentUsername());
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
                    if (Utils.isTeacher(Utils.getAuthentication()) &&
                            !course.getTeacher().getUsername().equals(Utils.currentUsername())) {
                        log.warn("Nauczyciel {} próbował dodać pytanie do quizu ID: {} bez uprawnień", Utils.currentUsername(), quizId);
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

    @PutMapping("/quizzes/{quizId}/questions/{questionId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<?> updateQuizQuestion(@PathVariable Long quizId, @PathVariable Long questionId, @RequestBody QuizQuestion question) {
        log.info("Próba edycji pytania ID: {} w quizie ID: {} przez użytkownika {}", questionId, quizId, Utils.currentUsername());
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
                    if (Utils.isTeacher(Utils.getAuthentication()) &&
                            !course.getTeacher().getUsername().equals(Utils.currentUsername())) {
                        log.warn("Nauczyciel {} próbował edytować pytanie ID: {} bez uprawnień", Utils.currentUsername(), questionId);
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
        log.info("Próba usunięcia pytania ID: {} z quizu ID: {} przez użytkownika {}", questionId, quizId, Utils.currentUsername());
        return quizQuestionRepository.findById(questionId)
                .map(question -> {
                    if (!question.getQuiz().getId().equals(quizId)) {
                        log.warn("Pytanie ID: {} nie należy do quizu ID: {}", questionId, quizId);
                        return ResponseEntity.badRequest()
                                .body(Map.of("success", false, "message", "Pytanie nie należy do tego quizu"));
                    }
                    Course course = question.getQuiz().getCourse();
                    if (Utils.isTeacher(Utils.getAuthentication()) &&
                            !course.getTeacher().getUsername().equals(Utils.currentUsername())) {
                        log.warn("Nauczyciel {} próbował usunąć pytanie ID: {} bez uprawnień", Utils.currentUsername(), questionId);
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

    @GetMapping("/{courseId}/available-quizzes")
    public ResponseEntity<?> getAvailableQuizzes(@PathVariable Long courseId) {
        Long userId = Utils.getCurrentUserId(userRepository);
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
                    Collections.shuffle(allQuestions);
                    int questionsToDisplay = Math.min(quiz.getNumberOfQuestionsToDisplay(), allQuestions.size());
                    List<QuizQuestionDTO> selectedQuestions = allQuestions.stream()
                            .limit(questionsToDisplay)
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
                                    "questions", selectedQuestions
                            )
                    ));
                })
                .orElseGet(() -> {
                    log.warn("Quiz z ID {} nie znaleziony", quizId);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping("/quizzes/{quizId}/edit")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<?> getQuizForEdit(@PathVariable Long quizId) {
        String username = Utils.currentUsername();
        log.info("Próba pobrania quizu ID: {} do edycji przez użytkownika {}", quizId, username);

        Optional<Quiz> quizOptional = quizRepository.findById(quizId);

        if (quizOptional.isEmpty()) {
            log.error("Quiz ID: {} nie znaleziony", quizId);
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "Quiz nie znaleziony"));
        }

        Quiz quiz = quizOptional.get();
        Authentication auth = Utils.getAuthentication();

        if (Utils.isTeacher(auth)) {
            Course course = quiz.getCourse();
            if (course == null || course.getTeacher() == null || !course.getTeacher().getUsername().equals(username)) {
                log.warn("Nauczyciel {} próbował pobrać quiz ID: {} bez uprawnień (quiz nie należy do jego kursu)", username, quizId);
                return ResponseEntity.status(403)
                        .body(Map.of("success", false, "message", "Brak dostępu do edycji tego quizu. Quiz nie należy do Twojego kursu."));
            }
        }

        List<QuizQuestion> allQuestions = quizQuestionRepository.findByQuizId(quizId);
        List<Map<String, Object>> questionMaps = allQuestions.stream()
                .map(q -> Map.of(
                        "questionId", q.getId(),
                        "questionText", q.getQuestionText(),
                        "questionType", q.getQuestionType(),
                        "options", q.getOptions() != null ? q.getOptions() : Map.of(),
                        "correctAnswer", q.getCorrectAnswer() != null ? q.getCorrectAnswer() : ""
                ))
                .collect(Collectors.toList());

        log.info("Quiz ID: {} z wszystkimi pytaniami pobrany pomyślnie do edycji dla {}", quizId, username);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "quiz", Map.of(
                        "id", quiz.getId(),
                        "title", quiz.getTitle(),
                        "description", quiz.getDescription(),
                        "numberOfQuestionsToDisplay", quiz.getNumberOfQuestionsToDisplay(),
                        "questions", questionMaps
                )
        ));
    }
}
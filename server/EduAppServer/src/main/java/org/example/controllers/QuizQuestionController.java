/**
 * @file QuizQuestionController.java
 * @brief Kontroler odpowiedzialny za zarządzanie pytaniami quizowymi w systemie.
 *
 * Udostępnia endpointy do dodawania, aktualizowania, usuwania oraz pobierania pytań quizowych,
 * a także pobierania dostępnych quizów i szczegółów quizów do edycji lub rozwiązywania.
 */
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

/**
 * @brief Kontroler odpowiedzialny za zarządzanie pytaniami quizowymi w systemie.
 *
 * Udostępnia endpointy do dodawania, aktualizowania, usuwania oraz pobierania pytań quizowych,
 * a także pobierania dostępnych quizów i szczegółów quizów do edycji lub rozwiązywania.
 */
@Slf4j
@RestController
@RequestMapping("/api/courses")
public class QuizQuestionController {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final UserRepository userRepository;
    private final UserCourseRepository userCourseRepository;

    /**
     * @brief Konstruktor klasy QuizQuestionController.
     * @param quizRepository Repozytorium quizów.
     * @param quizQuestionRepository Repozytorium pytań quizowych.
     * @param userRepository Repozytorium użytkowników.
     * @param userCourseRepository Repozytorium powiązań użytkowników z kursami.
     */
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

    /**
     * @brief Dodaje nowe pytanie do określonego quizu.
     * @param quizId ID quizu.
     * @param question Obiekt QuizQuestion zawierający dane pytania.
     * @return ResponseEntity z wynikiem operacji:
     * - success (boolean) - czy operacja się powiodła
     * - message (String) - komunikat o wyniku
     * - question (QuizQuestion) - dodane pytanie (w przypadku sukcesu)
     */
    @PostMapping("/quizzes/{quizId}/questions")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<?> addQuizQuestion(@PathVariable Long quizId, @RequestBody QuizQuestion question) {
        if (question.getQuestionText() == null || question.getQuestionText().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Treść pytania jest wymagana"));
        }
        if (question.getQuestionType() == null ||
                (!question.getQuestionType().equals("multiple_choice") &&
                        !question.getQuestionType().equals("open_ended") &&
                        !question.getQuestionType().equals("true_false"))) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Typ pytania musi być 'multiple_choice', 'open_ended' lub 'true_false'"));
        }
        if (question.getQuestionType().equals("multiple_choice")) {
            if (question.getOptions() == null || question.getOptions().size() < 2) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Pytania wielokrotnego wyboru wymagają co najmniej 2 opcji"));
            }
            if (question.getCorrectAnswer() == null || question.getCorrectAnswer().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Co najmniej jedna poprawna odpowiedź jest wymagana"));
            }
            List<String> correctAnswers = Arrays.asList(question.getCorrectAnswer().split(","));
            for (String correct : correctAnswers) {
                if (!question.getOptions().containsKey(correct)) {
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
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Opcje dla pytania prawda/fałsz muszą być dokładnie 'True: Prawda' i 'False: Fałsz'"));
            }
            if (question.getCorrectAnswer() == null || question.getCorrectAnswer().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Co najmniej jedna poprawna odpowiedź jest wymagana"));
            }
            List<String> correctAnswers = Arrays.asList(question.getCorrectAnswer().split(","));
            for (String correct : correctAnswers) {
                if (!Set.of("True", "False").contains(correct)) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("success", false, "message", "Poprawne odpowiedzi muszą być 'True' lub 'False'"));
                }
            }
        } else if (question.getQuestionType().equals("open_ended")) {
            if (question.getCorrectAnswer() == null || question.getCorrectAnswer().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Poprawna odpowiedź jest wymagana dla pytań otwartych"));
            }
            if (question.getOptions() != null && !question.getOptions().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Pytania otwarte nie mogą zawierać opcji"));
            }
        }

        return quizRepository.findById(quizId)
                .map(quiz -> {
                    Course course = quiz.getCourse();
                    if (Utils.isTeacher(Utils.getAuthentication()) &&
                            !course.getTeacher().getUsername().equals(Utils.currentUsername())) {
                        return ResponseEntity.status(403)
                                .body(Map.of("success", false, "message", "Brak dostępu do tego quizu"));
                    }
                    question.setQuiz(quiz);
                    QuizQuestion savedQuestion = quizQuestionRepository.save(question);
                    log.info("Dodano pytanie '{}' do quizu ID: {}", question.getQuestionText(), quizId);
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Pytanie dodane pomyślnie",
                            "question", savedQuestion
                    ));
                })
                .orElse(ResponseEntity.status(404)
                        .body(Map.of("success", false, "message", "Quiz nie znaleziony")));
    }

    /**
     * @brief Aktualizuje istniejące pytanie w quizie.
     * @param quizId ID quizu.
     * @param questionId ID pytania do zaktualizowania.
     * @param question Obiekt QuizQuestion zawierający zaktualizowane dane pytania.
     * @return ResponseEntity z wynikiem operacji:
     * - success (boolean) - czy operacja się powiodła
     * - message (String) - komunikat o wyniku
     * - question (QuizQuestion) - zaktualizowane pytanie (w przypadku sukcesu)
     */
    @PutMapping("/quizzes/{quizId}/questions/{questionId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<?> updateQuizQuestion(@PathVariable Long quizId, @PathVariable Long questionId, @RequestBody QuizQuestion question) {
        if (question.getQuestionText() == null || question.getQuestionText().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Treść pytania jest wymagana"));
        }
        if (question.getQuestionType() == null ||
                (!question.getQuestionType().equals("multiple_choice") &&
                        !question.getQuestionType().equals("open_ended") &&
                        !question.getQuestionType().equals("true_false"))) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Typ pytania musi być 'multiple_choice', 'open_ended' lub 'true_false'"));
        }
        if (question.getQuestionType().equals("multiple_choice")) {
            if (question.getOptions() == null || question.getOptions().size() < 2) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Pytania wielokrotnego wyboru wymagają co najmniej 2 opcji"));
            }
            if (question.getCorrectAnswer() == null || question.getCorrectAnswer().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Co najmniej jedna poprawna odpowiedź jest wymagana"));
            }
            List<String> correctAnswers = Arrays.asList(question.getCorrectAnswer().split(","));
            for (String correct : correctAnswers) {
                if (!question.getOptions().containsKey(correct)) {
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
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Opcje dla pytania prawda/fałsz muszą być dokładnie 'True: Prawda' i 'False: Fałsz'"));
            }
            if (question.getCorrectAnswer() == null || question.getCorrectAnswer().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Co najmniej jedna poprawna odpowiedź jest wymagana"));
            }
            List<String> correctAnswers = Arrays.asList(question.getCorrectAnswer().split(","));
            for (String correct : correctAnswers) {
                if (!Set.of("True", "False").contains(correct)) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("success", false, "message", "Poprawne odpowiedzi muszą być 'True' lub 'False'"));
                }
            }
        } else if (question.getQuestionType().equals("open_ended")) {
            if (question.getCorrectAnswer() == null || question.getCorrectAnswer().isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Poprawna odpowiedź jest wymagana dla pytań otwartych"));
            }
            if (question.getOptions() != null && !question.getOptions().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Pytania otwarte nie mogą zawierać opcji"));
            }
        }

        return quizQuestionRepository.findById(questionId)
                .map(existingQuestion -> {
                    if (!existingQuestion.getQuiz().getId().equals(quizId)) {
                        return ResponseEntity.badRequest()
                                .body(Map.of("success", false, "message", "Pytanie nie należy do tego quizu"));
                    }
                    Course course = existingQuestion.getQuiz().getCourse();
                    if (Utils.isTeacher(Utils.getAuthentication()) &&
                            !course.getTeacher().getUsername().equals(Utils.currentUsername())) {
                        return ResponseEntity.status(403)
                                .body(Map.of("success", false, "message", "Brak dostępu do tego quizu"));
                    }
                    existingQuestion.setQuestionText(question.getQuestionText());
                    existingQuestion.setQuestionType(question.getQuestionType());
                    existingQuestion.setOptions(question.getOptions());
                    existingQuestion.setCorrectAnswer(question.getCorrectAnswer());
                    QuizQuestion updatedQuestion = quizQuestionRepository.save(existingQuestion);
                    log.info("Zaktualizowano pytanie ID: {} w quizie ID: {}", questionId, quizId);
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Pytanie zaktualizowane pomyślnie",
                            "question", updatedQuestion
                    ));
                })
                .orElse(ResponseEntity.status(404)
                        .body(Map.of("success", false, "message", "Pytanie nie znalezione")));
    }

    /**
     * @brief Usuwa pytanie z quizu.
     * @param quizId ID quizu.
     * @param questionId ID pytania do usunięcia.
     * @return ResponseEntity z wynikiem operacji:
     * - success (boolean) - czy operacja się powiodła
     * - message (String) - komunikat o wyniku
     */
    @DeleteMapping("/quizzes/{quizId}/questions/{questionId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Transactional
    public ResponseEntity<?> deleteQuizQuestion(@PathVariable Long quizId, @PathVariable Long questionId) {
        return quizQuestionRepository.findById(questionId)
                .map(question -> {
                    if (!question.getQuiz().getId().equals(quizId)) {
                        return ResponseEntity.badRequest()
                                .body(Map.of("success", false, "message", "Pytanie nie należy do tego quizu"));
                    }
                    Course course = question.getQuiz().getCourse();
                    if (Utils.isTeacher(Utils.getAuthentication()) &&
                            !course.getTeacher().getUsername().equals(Utils.currentUsername())) {
                        return ResponseEntity.status(403)
                                .body(Map.of("success", false, "message", "Brak dostępu do tego quizu"));
                    }
                    quizQuestionRepository.delete(question);
                    log.info("Usunięto pytanie ID: {} z quizu ID: {}", questionId, quizId);
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Pytanie usunięte pomyślnie"
                    ));
                })
                .orElse(ResponseEntity.status(404)
                        .body(Map.of("success", false, "message", "Pytanie nie znaleziono")));
    }

    /**
     * @brief Pobiera listę dostępnych quizów dla danego kursu.
     * @param courseId ID kursu.
     * @return ResponseEntity z wynikiem operacji:
     * - success (boolean) - czy operacja się powiodła
     * - message (String) - komunikat o błędzie (jeśli dotyczy)
     * - quizzes (List<Quiz>) - lista dostępnych quizów (w przypadku sukcesu)
     */
    @GetMapping("/{courseId}/available-quizzes")
    public ResponseEntity<?> getAvailableQuizzes(@PathVariable Long courseId) {
        Long userId = Utils.getCurrentUserId(userRepository);

        if (!userCourseRepository.existsByUserIdAndCourseId(userId, courseId)) {
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "Nie jesteś zapisany na ten kurs"));
        }

        List<Quiz> quizzes = quizRepository.findByCourseId(courseId);
        log.info("Pobrano dostępne quizy dla kursu ID: {} przez użytkownika ID: {}", courseId, userId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "quizzes", quizzes
        ));
    }

    /**
     * @brief Pobiera szczegóły quizu do rozwiązania przez użytkownika.
     * @param quizId ID quizu.
     * @return ResponseEntity z wynikiem operacji:
     * - success (boolean) - czy operacja się powiodła
     * - quiz (Map) - szczegóły quizu:
     *   - id (Long) - ID quizu
     *   - title (String) - tytuł quizu
     *   - description (String) - opis quizu
     *   - questions (List<QuizQuestionDTO>) - lista wybranych pytań
     */
    @GetMapping("/quizzes/{quizId}")
    public ResponseEntity<?> getQuizForSolving(@PathVariable Long quizId) {
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

                    log.info("Pobrano quiz ID: {} do rozwiązania", quizId);
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
                .orElse(ResponseEntity.status(404)
                        .body(Map.of("success", false, "message", "Quiz nie znaleziony")));
    }

    /**
     * @brief Pobiera szczegóły quizu do edycji przez nauczyciela lub administratora.
     * @param quizId ID quizu.
     * @return ResponseEntity z wynikiem operacji:
     * - success (boolean) - czy operacja się powiodła
     * - message (String) - komunikat o błędzie (jeśli dotyczy)
     * - quiz (Map) - szczegóły quizu:
     *   - id (Long) - ID quizu
     *   - title (String) - tytuł quizu
     *   - description (String) - opis quizu
     *   - numberOfQuestionsToDisplay (Integer) - liczba pytań do wyświetlenia
     *   - questions (List<Map>) - lista pytań z szczegółami
     */
    @GetMapping("/quizzes/{quizId}/edit")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<?> getQuizForEdit(@PathVariable Long quizId) {
        String username = Utils.currentUsername();

        Optional<Quiz> quizOptional = quizRepository.findById(quizId);

        if (quizOptional.isEmpty()) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "Quiz nie znaleziony"));
        }

        Quiz quiz = quizOptional.get();
        Authentication auth = Utils.getAuthentication();

        if (Utils.isTeacher(auth)) {
            Course course = quiz.getCourse();
            if (course == null || course.getTeacher() == null || !course.getTeacher().getUsername().equals(username)) {
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

        log.info("Pobrano quiz ID: {} do edycji przez użytkownika {}", quizId, username);
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
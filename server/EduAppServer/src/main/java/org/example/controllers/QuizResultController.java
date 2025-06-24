
package org.example.controllers;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.example.DataBaseRepositories.*;
import org.example.PdfGenerationService;
import org.example.database.*;
import org.example.dto.QuizAnswerDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @brief Kontroler REST do obsługi operacji związanych z wynikami quizów.
 */
@Slf4j
@RestController
@RequestMapping("/api/courses")
public class QuizResultController {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizResultRepository quizResultRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final UserRepository userRepository;
    private final PdfGenerationService pdfGenerationService;

    /**
     * @brief Konstruktor wstrzykujący zależności.
     * @param quizRepository Repozytorium quizów.
     * @param quizQuestionRepository Repozytorium pytań quizowych.
     * @param quizResultRepository Repozytorium wyników quizów.
     * @param quizAnswerRepository Repozytorium odpowiedzi quizowych.
     * @param userRepository Repozytorium użytkowników.
     * @param pdfGenerationService Serwis do generowania PDF.
     */
    @Autowired
    public QuizResultController(QuizRepository quizRepository,
                                QuizQuestionRepository quizQuestionRepository,
                                QuizResultRepository quizResultRepository,
                                QuizAnswerRepository quizAnswerRepository,
                                UserRepository userRepository, PdfGenerationService pdfGenerationService) {
        this.quizRepository = quizRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.quizResultRepository = quizResultRepository;
        this.quizAnswerRepository = quizAnswerRepository;
        this.userRepository = userRepository;
        this.pdfGenerationService = pdfGenerationService;
    }

    /**
     * @brief Waliduje odpowiedź dla pytania wielokrotnego wyboru.
     * @param question Obiekt pytania quizowego.
     * @param answer Odpowiedź użytkownika.
     * @return true jeśli odpowiedź jest poprawna, false w przeciwnym razie.
     */
    private boolean validateMultipleChoice(QuizQuestion question, String answer) {
        Set<String> correctAnswers = Set.of(question.getCorrectAnswer().split(","));
        Set<String> userAnswers = Set.of(answer.split(","));
        return correctAnswers.equals(userAnswers);
    }

    /**
     * @brief Waliduje odpowiedź dla pytania typu prawda/fałsz.
     * @param question Obiekt pytania quizowego.
     * @param answer Odpowiedź użytkownika.
     * @return true jeśli odpowiedź jest poprawna, false w przeciwnym razie.
     */
    private boolean validateTrueFalse(QuizQuestion question, String answer) {
        return question.getCorrectAnswer().equalsIgnoreCase(answer);
    }

    /**
     * @brief Waliduje odpowiedź dla pytania otwartego.
     * @param question Obiekt pytania quizowego.
     * @param answer Odpowiedź użytkownika.
     * @return true jeśli odpowiedź jest poprawna, false w przeciwnym razie.
     */
    private boolean validateOpenEnded(QuizQuestion question, String answer) {
        return answer.trim().equalsIgnoreCase(question.getCorrectAnswer().trim());
    }

    /**
     * @brief Waliduje odpowiedź użytkownika w zależności od typu pytania.
     * @param answer Obiekt DTO zawierający ID pytania i odpowiedź użytkownika.
     * @return true jeśli odpowiedź jest poprawna, false w przeciwnym razie.
     * @throws RuntimeException jeśli pytanie nie zostanie znalezione.
     * @throws IllegalArgumentException jeśli typ pytania jest nieznany.
     */
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

    /**
     * @brief Przetwarza przesłane odpowiedzi na quiz.
     * @param quizId ID quizu.
     * @param answers Lista odpowiedzi użytkownika.
     * @return ResponseEntity zawierający wynik przesłania quizu.
     */
    @PostMapping("/quizzes/{quizId}/submit")
    public ResponseEntity<?> submitQuizAnswers(
            @PathVariable Long quizId,
            @RequestBody List<QuizAnswerDTO> answers
    ) {
        Long userId = Utils.getCurrentUserId(userRepository);
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
                    Utils.getCurrentUser(userRepository),
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

    /**
     * @brief Pobiera wyniki quizu dla aktualnie zalogowanego użytkownika.
     * @param quizId ID quizu.
     * @return ResponseEntity zawierający wyniki quizu.
     */
    @GetMapping("/quizzes/{quizId}/results")
    public ResponseEntity<?> getQuizResults(@PathVariable Long quizId) {
        Long userId = Utils.getCurrentUserId(userRepository);
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

    /**
     * @brief Pobiera szczegółowe wyniki dla danego quizu (dostępne tylko dla TEACHER/ADMIN).
     * @param quizId ID quizu.
     * @return ResponseEntity zawierający szczegółowe wyniki quizu.
     */
    @GetMapping("/quizzes/{quizId}/detailed-results")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<?> getQuizDetailedResults(@PathVariable Long quizId) {
        log.info("Pobieranie szczegółowych wyników dla quizu ID: {} przez użytkownika {}", quizId, Utils.currentUsername());

        return quizRepository.findById(quizId)
                .map(quiz -> {
                    if (Utils.isTeacher(Utils.getAuthentication()) &&
                            !quiz.getCourse().getTeacher().getUsername().equals(Utils.currentUsername())) {
                        log.warn("Nauczyciel {} próbował uzyskać dostęp do szczegółowych wyników quizu ID: {} bez uprawnień", Utils.currentUsername(), quizId);
                        return ResponseEntity.status(403).body(Map.of(
                                "success", false,
                                "message", "Brak dostępu do tego quizu"));
                    }

                    List<QuizResult> results = quizResultRepository.findByQuizId(quizId);
                    List<Map<String, Object>> detailedResults = results.stream().map(result -> {
                        List<QuizAnswer> answers = quizAnswerRepository.findByQuizResultIdWithQuestions(result.getId());
                        Map<String, Object> studentResult = new HashMap<>();
                        studentResult.put("resultId", result.getId());
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

    /**
     * @brief Usuwa wynik quizu (dostępne tylko dla TEACHER/ADMIN).
     * @param resultId ID wyniku quizu do usunięcia.
     * @return ResponseEntity z informacją o sukcesie lub błędzie.
     */
    @DeleteMapping("/quizzes/results/{resultId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Transactional
    public ResponseEntity<?> deleteQuizResult(@PathVariable Long resultId) {
        Long currentUserId = Utils.getCurrentUserId(userRepository);
        log.info("Użytkownik {} próbuje usunąć wynik quizu o ID: {}", currentUserId, resultId);

        // Pobranie obiektu Authentication z Twojej klasy Utils
        Authentication authentication = Utils.getAuthentication();

        // Wykorzystanie istniejącej funkcji z Utils.java do sprawdzenia roli ADMIN
        boolean isAdmin = Utils.isAdmin(authentication);

        return quizResultRepository.findById(resultId)
                .map(result -> {
                    String teacherUsername = result.getQuiz().getCourse().getTeacher().getUsername();

                    // Warunek pozostaje ten sam: zablokuj, jeśli użytkownik NIE jest adminem ORAZ nie jest właściwym nauczycielem
                    if (!isAdmin && (!Utils.isTeacher(authentication) || !teacherUsername.equals(Utils.currentUsername()))) {
                        log.warn("Brak uprawnień do usunięcia wyniku ID: {}. Użytkownik nie jest adminem ani właścicielem kursu.", resultId);
                        return ResponseEntity.status(403).body(Map.of("success", false, "message", "Brak uprawnień"));
                    }

                    // Logika usuwania - dostępna dla admina lub właściwego nauczyciela
                    quizAnswerRepository.deleteByQuizResultId(resultId);
                    quizResultRepository.delete(result);

                    log.info("Wynik quizu ID: {} został pomyślnie usunięty przez użytkownika {}.", resultId, Utils.currentUsername());
                    return ResponseEntity.ok(Map.of("success", true, "message", "Wynik został usunięty."));
                })
                .orElseGet(() -> {
                    log.error("Nie znaleziono wyniku quizu o ID: {}", resultId);
                    return ResponseEntity.status(404).body(Map.of("success", false, "message", "Wynik nie znaleziony."));
                });
    }
    /**
     * @brief Generuje i pobiera raport PDF ze szczegółowymi wynikami quizu (dostępne tylko dla TEACHER/ADMIN).
     * @param quizId ID quizu.
     * @return ResponseEntity zawierający strumień danych PDF.
     */
    @GetMapping("/quizzes/{quizId}/detailed-results/pdf")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<InputStreamResource> downloadQuizResultsPdf(@PathVariable Long quizId) {
        log.info("Generowanie raportu PDF dla quizu ID: {}", quizId);
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz nie znaleziony"));

        if (Utils.isTeacher(Utils.getAuthentication()) &&
                !quiz.getCourse().getTeacher().getUsername().equals(Utils.currentUsername())) {
            log.warn("Nauczyciel {} próbował pobrać PDF dla quizu ID: {} bez uprawnień", Utils.currentUsername(), quizId);
            return ResponseEntity.status(403).build();
        }

        List<QuizResult> results = quizResultRepository.findByQuizId(quizId);
        results.forEach(result -> result.setQuizAnswers(quizAnswerRepository.findByQuizResultIdWithQuestions(result.getId())));


        ByteArrayInputStream bis = pdfGenerationService.generateQuizResultsPdf(quiz, results);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=quiz_results_" + quizId + ".pdf");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(bis));
    }
}
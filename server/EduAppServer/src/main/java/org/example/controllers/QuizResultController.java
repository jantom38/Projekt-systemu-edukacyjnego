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
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/courses")
public class QuizResultController {

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizResultRepository quizResultRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final UserRepository userRepository;
    private final PdfGenerationService pdfGenerationService; // Wstrzyknij serwis


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
    @DeleteMapping("/quizzes/results/{resultId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Transactional
    public ResponseEntity<?> deleteQuizResult(@PathVariable Long resultId) {
        Long currentUserId = Utils.getCurrentUserId(userRepository);
        log.info("Użytkownik {} próbuje usunąć wynik quizu o ID: {}", currentUserId, resultId);

        return quizResultRepository.findById(resultId)
                .map(result -> {
                    // Weryfikacja uprawnień - czy nauczyciel jest właścicielem kursu
                    String teacherUsername = result.getQuiz().getCourse().getTeacher().getUsername();
                    if (!Utils.isTeacher(Utils.getAuthentication()) || !teacherUsername.equals(Utils.currentUsername())) {
                        log.warn("Brak uprawnień do usunięcia wyniku ID: {}", resultId);
                        return ResponseEntity.status(403).body(Map.of("success", false, "message", "Brak uprawnień"));
                    }

                    // Najpierw usuń powiązane odpowiedzi, potem wynik
                    quizAnswerRepository.deleteByQuizResultId(resultId);
                    quizResultRepository.delete(result);

                    log.info("Wynik quizu ID: {} został pomyślnie usunięty.", resultId);
                    return ResponseEntity.ok(Map.of("success", true, "message", "Wynik został usunięty."));
                })
                .orElseGet(() -> {
                    log.error("Nie znaleziono wyniku quizu o ID: {}", resultId);
                    return ResponseEntity.status(404).body(Map.of("success", false, "message", "Wynik nie znaleziony."));
                });
    }

    @GetMapping("/quizzes/{quizId}/detailed-results/pdf")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<InputStreamResource> downloadQuizResultsPdf(@PathVariable Long quizId) {
        log.info("Generowanie raportu PDF dla quizu ID: {}", quizId);
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz nie znaleziony"));

        // Sprawdzenie uprawnień
        if (Utils.isTeacher(Utils.getAuthentication()) &&
                !quiz.getCourse().getTeacher().getUsername().equals(Utils.currentUsername())) {
            log.warn("Nauczyciel {} próbował pobrać PDF dla quizu ID: {} bez uprawnień", Utils.currentUsername(), quizId);
            return ResponseEntity.status(403).build();
        }

        List<QuizResult> results = quizResultRepository.findByQuizId(quizId);
        // Pobieramy odpowiedzi dla każdego wyniku, aby były dostępne w serwisie PDF
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

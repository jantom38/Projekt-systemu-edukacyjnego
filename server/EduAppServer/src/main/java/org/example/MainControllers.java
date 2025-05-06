package org.example;

import lombok.extern.slf4j.Slf4j;
import org.example.DataBaseRepositories.*;
import org.example.database.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Set;
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



@Autowired
public MainControllers(CourseRepository courseRepository,
                       CourseFileRepository courseFileRepository,
                       UserRepository userRepository,
                       QuizRepository quizRepository, UserCourseRepository userCourseRepository, QuizQuestionRepository quizQuestionRepository) {
    this.courseRepository = courseRepository;
    this.courseFileRepository = courseFileRepository;
    this.userRepository = userRepository;
    this.quizRepository = quizRepository;

    this.userCourseRepository = userCourseRepository;
    this.quizQuestionRepository = quizQuestionRepository;
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

@GetMapping("/{courseId}/quizzes")
public ResponseEntity<?> getQuizzesForCourse(@PathVariable Long courseId) {
    log.info("Pobieranie quizów dla kursu ID: {}", courseId);
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (isTeacher(auth)) {
        boolean owns = courseRepository.findByIdAndTeacherUsername(courseId, currentUsername()).isPresent();
        if (!owns) {
            log.warn("Nauczyciel {} próbował uzyskać dostęp do quizów kursu ID: {} bez uprawnień", currentUsername(), courseId);
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "Brak dostępu do tego kursu"));
        }
    } else {
        User user = userRepository.findByUsername(currentUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        boolean enrolled = userCourseRepository.existsByUserIdAndCourseId(user.getId(), courseId);
        if (!enrolled) {
            return ResponseEntity.status(403).body(Map.of(
                    "success", false,
                    "message", "Nie jesteś zapisany na ten kurs"));
        }
    }

    if (!courseRepository.existsById(courseId)) {
        log.error("Kurs ID: {} nie znaleziony", courseId);
        return ResponseEntity.status(404).body(Map.of(
                "success", false,
                "message", "Course not found"));
    }

    List<Quiz> quizzes = quizRepository.findByCourseId(courseId);
    log.info("Pobrano {} quizów dla kursu ID: {}", quizzes.size(), courseId);
    return ResponseEntity.ok(Map.of(
            "success", true,
            "quizzes", quizzes
    ));
}

@PostMapping("/{courseId}/quizzes")
@PreAuthorize("hasRole('TEACHER')")
public ResponseEntity<?> addQuizToCourse(@PathVariable Long courseId, @RequestBody Quiz quiz) {
    log.info("Próba dodania quizu do kursu ID: {} przez nauczyciela {}", courseId, currentUsername());
    if (quiz.getTitle() == null || quiz.getTitle().isBlank()) {
        log.warn("Próba dodania quizu z pustym tytułem dla kursu ID: {}", courseId);
        return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", "Tytuł quizu jest wymagany"));
    }

    return courseRepository.findByIdAndTeacherUsername(courseId, currentUsername())
            .map(course -> {
                quiz.setCourse(course);
                Quiz savedQuiz = quizRepository.save(quiz);
                log.info("Quiz '{}' (ID: {}) dodany do kursu ID: {}", savedQuiz.getTitle(), savedQuiz.getId(), courseId);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Quiz added successfully",
                        "quiz", savedQuiz
                ));
            })
            .orElseGet(() -> {
                log.error("Brak dostępu do kursu ID: {} lub kurs nie istnieje dla nauczyciela {}", courseId, currentUsername());
                return ResponseEntity.status(403)
                        .body(Map.of("success", false, "message", "Brak dostępu do tego kursu lub kurs nie istnieje"));
            });
}

@GetMapping("/quizzes/{quizId}/questions")
public ResponseEntity<?> getQuestionsForQuiz(@PathVariable Long quizId) {
    log.info("Pobieranie pytań dla quizu ID: {}", quizId);
    return quizRepository.findById(quizId)
            .map(quiz -> {
                List<QuizQuestion> questions = quizQuestionRepository.findByQuizId(quizId);
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "questions", questions
                ));
            })
            .orElseGet(() -> {
                log.error("Quiz ID: {} nie znaleziony", quizId);
                return ResponseEntity.status(404)
                        .body(Map.of("success", false, "message", "Quiz not found"));
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
        if (question.getQuestionType().equals("multiple_choice") || question.getQuestionType().equals("true_false")) {
            if (question.getOptions() == null || question.getOptions().isEmpty()) {
                log.warn("Brak opcji dla pytania wielokrotnego wyboru lub prawda/fałsz w quizie ID: {}", quizId);
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Opcje są wymagane dla pytań wielokrotnego wyboru lub prawda/fałsz"));
            }
            if (question.getCorrectAnswer() == null || !question.getOptions().containsKey(question.getCorrectAnswer())) {
                log.warn("Nieprawidłowa poprawna odpowiedź dla pytania w quizie ID: {}", quizId);
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Poprawna odpowiedź musi być jednym z kluczy opcji"));
            }
            if (question.getQuestionType().equals("true_false")) {
                if (!question.getOptions().keySet().equals(Set.of("True", "False"))) {
                    log.warn("Nieprawidłowe opcje dla pytania prawda/fałsz w quizie ID: {}", quizId);
                    return ResponseEntity.badRequest()
                            .body(Map.of("success", false, "message", "Opcje dla pytania prawda/fałsz muszą być 'True' i 'False'"));
                }
            }
        } else if (question.getQuestionType().equals("open_ended")) {
            if (question.getCorrectAnswer() == null || question.getCorrectAnswer().isBlank()) {
                log.warn("Brak poprawnej odpowiedzi dla pytania otwartego w quizie ID: {}", quizId);
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Poprawna odpowiedź jest wymagana dla pytań otwartych"));
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
}

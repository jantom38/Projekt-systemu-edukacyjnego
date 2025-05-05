package org.example;

import lombok.extern.slf4j.Slf4j;
import org.example.DataBaseRepositories.*;
import org.example.database.Course;
import org.example.database.Quiz;
import org.example.database.User;
import org.example.database.UserCourse;
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

    @Autowired
    public MainControllers(CourseRepository courseRepository,
                           CourseFileRepository courseFileRepository,
                           UserRepository userRepository,
                           QuizRepository quizRepository, UserCourseRepository userCourseRepository) {
        this.courseRepository = courseRepository;
        this.courseFileRepository = courseFileRepository;
        this.userRepository = userRepository;
        this.quizRepository = quizRepository;

        this.userCourseRepository = userCourseRepository;
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

    @GetMapping("/{id}/quizzes")
    public ResponseEntity<?> getCourseQuizzes(@PathVariable Long id) {
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

        List<Quiz> quizzes = quizRepository.findByCourseId(id);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "quizzes", quizzes
        ));
    }

    @PostMapping("/{id}/quizzes")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> addQuiz(@PathVariable Long id, @RequestBody Quiz quiz) {
        if (quiz.getTitle() == null || quiz.getTitle().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Tytuł quizu jest wymagany"));
        }

        return courseRepository.findByIdAndTeacherUsername(id, currentUsername())
                .map(course -> {
                    quiz.setCourse(course);
                    Quiz savedQuiz = quizRepository.save(quiz);
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Quiz added successfully",
                            "quiz", savedQuiz
                    ));
                })
                .orElse(ResponseEntity.status(403)
                        .body(Map.of("success", false, "message", "Brak dostępu do tego kursu lub kurs nie istnieje")));
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
}

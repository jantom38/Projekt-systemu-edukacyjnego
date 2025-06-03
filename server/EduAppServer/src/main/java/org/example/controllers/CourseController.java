package org.example.controllers;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.example.DataBaseRepositories.*;
import org.example.database.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    @Value("${file.upload-dir:#{systemProperties['user.dir'] + '/uploads'}}")
    private String uploadDir;

    private final CourseRepository courseRepository;
    private final CourseFileRepository courseFileRepository;
    private final UserRepository userRepository;
    private final UserCourseRepository userCourseRepository;

    @Autowired
    public CourseController(CourseRepository courseRepository,
                            CourseFileRepository courseFileRepository,
                            UserRepository userRepository,
                            UserCourseRepository userCourseRepository) {
        this.courseRepository = courseRepository;
        this.courseFileRepository = courseFileRepository;
        this.userRepository = userRepository;
        this.userCourseRepository = userCourseRepository;
    }

    @GetMapping
    public List<Course> getAllCourses() {
        Authentication auth = Utils.getAuthentication();
        if (Utils.isTeacher(auth)) {
            return courseRepository.findByTeacherUsername(Utils.currentUsername());
        } else if (Utils.isAdmin(auth)) {
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

        User teacher = userRepository.findByUsername(Utils.currentUsername())
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
        String username = Utils.currentUsername();

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
        Authentication auth = Utils.getAuthentication();
        if (Utils.isTeacher(auth)) {
            boolean owns = courseRepository.findByIdAndTeacherUsername(id, Utils.currentUsername()).isPresent();
            if (!owns) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "message", "Brak dostępu do tego kursu"));
            }
        } else if (Utils.isAdmin(auth)) {
            log.info("Admin {} uzyskuje dostęp do plików kursu ID: {}", Utils.currentUsername(), id);
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

        return ResponseEntity.ok(courseFileRepository.findByCourseId(id));
    }

    @DeleteMapping("/{courseId}/files/{fileId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<?> deleteCourseFile(@PathVariable Long courseId,
                                              @PathVariable Long fileId) {
        Authentication auth = Utils.getAuthentication();
        if (Utils.isTeacher(auth) && courseRepository.findByIdAndTeacherUsername(courseId, Utils.currentUsername()).isEmpty()) {
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
                        .body(Map.of("success", false, "message", "Plik nie znaleziony dla tego kursu")));
    }

    @GetMapping("/my-courses")
    public ResponseEntity<?> getUserCourses() {
        String username = Utils.currentUsername();
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
        Authentication auth = Utils.getAuthentication();
        if (Utils.isTeacher(auth) && courseRepository.findByIdAndTeacherUsername(id, Utils.currentUsername()).isEmpty()) {
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

    @GetMapping("/{courseId}/users")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<?> getCourseUsers(@PathVariable Long courseId) {
        log.info("Pobieranie użytkowników kursu ID: {} przez użytkownika {}", courseId, Utils.currentUsername());

        Authentication auth = Utils.getAuthentication();
        if (Utils.isTeacher(auth) && courseRepository.findByIdAndTeacherUsername(courseId, Utils.currentUsername()).isEmpty()) {
            log.warn("Brak dostępu do kursu ID: {} dla nauczyciela {}", courseId, Utils.currentUsername());
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
        log.info("Próba usunięcia użytkownika ID: {} z kursu ID: {} przez {}", userId, courseId, Utils.currentUsername());

        Authentication auth = Utils.getAuthentication();
        if (Utils.isTeacher(auth) && courseRepository.findByIdAndTeacherUsername(courseId, Utils.currentUsername()).isEmpty()) {
            log.warn("Brak dostępu do kursu ID: {} dla nauczyciela {}", courseId, Utils.currentUsername());
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
}
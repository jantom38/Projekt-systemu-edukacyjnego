package org.example;

import jakarta.validation.Valid;
import org.example.DataBaseRepositories.CourseFileRepository;
import org.example.DataBaseRepositories.CourseRepository;
import org.example.database.Course;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/courses")

public class ListOfCoursesModule {

    private final CourseRepository courseRepository;
    private final CourseFileRepository courseFileRepository;

    @Autowired
    public ListOfCoursesModule(CourseRepository courseRepository, CourseFileRepository courseFileRepository) {
        this.courseRepository = courseRepository;
        this.courseFileRepository = courseFileRepository;
    }

    @GetMapping
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }
    // W pliku ListOfCoursesModule.java
    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> addCourse(@RequestBody Course course) {
        // Sprawdzenie czy klucz dostępu został podany
        if (course.getAccessKey() == null || course.getAccessKey().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Klucz dostępu jest wymagany"));
        }

        Course savedCourse = courseRepository.save(course);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Course added successfully",
                "course", savedCourse
        ));
    }

    @PostMapping("/{id}/verify-key")
    public ResponseEntity<?> verifyAccessKey(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String providedKey = request.get("accessKey");
        return courseRepository.findById(id)
                .map(course -> {
                    if (course.getAccessKey().equals(providedKey)) {
                        return ResponseEntity.ok(Map.of("success", true, "message", "Access granted"));
                    } else {
                        return ResponseEntity.status(403)
                                .body(Map.of("success", false, "message", "Invalid access key"));
                    }
                })
                .orElse(ResponseEntity.status(404)
                        .body(Map.of("success", false, "message", "Course not found")));
    }

    @GetMapping("/{id}/files")
    public ResponseEntity<?> getCourseFiles(@PathVariable Long id) {
        if (!courseRepository.existsById(id)) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "Course not found",
                    "status", 404
            ));
        }
        return ResponseEntity.ok(courseFileRepository.findByCourseId(id));
    }
}
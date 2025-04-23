package org.example;

import org.example.DataBaseRepositories.CourseFileRepository;
import org.example.DataBaseRepositories.CourseRepository;
import org.example.database.Course;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
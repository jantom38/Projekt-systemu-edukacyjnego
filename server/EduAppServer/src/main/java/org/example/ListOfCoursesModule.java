package org.example;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.Logger;
import org.example.DataBaseRepositories.CourseFileRepository;
import org.example.DataBaseRepositories.CourseRepository;
import org.example.database.Course;
import org.example.database.CourseFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/courses")
public class ListOfCoursesModule {

    @Value("${file.upload-dir:#{systemProperties['user.dir'] + '/uploads'}}")
    private String uploadDir;

    private final CourseRepository courseRepository;
    private final CourseFileRepository courseFileRepository;

    @Autowired
    public ListOfCoursesModule(CourseRepository courseRepository,
                               CourseFileRepository courseFileRepository) {
        this.courseRepository = courseRepository;
        this.courseFileRepository = courseFileRepository;
    }

    @GetMapping
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }

    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> addCourse(@RequestBody Course course) {
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
    public ResponseEntity<?> verifyAccessKey(@PathVariable Long id,
                                             @RequestBody Map<String, String> request) {
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

    /**
     * Endpoint for deleting a course file. Only TEACHER role can call this.
     */
    @DeleteMapping("/{courseId}/files/{fileId}")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> deleteCourseFile(@PathVariable Long courseId,
                                              @PathVariable Long fileId) {
        // Check course exists
        if (!courseRepository.existsById(courseId)) {
            return ResponseEntity.status(404)
                    .body(Map.of("success", false, "message", "Course not found"));
        }

        return courseFileRepository.findById(fileId)
                .filter(cf -> cf.getCourse().getId().equals(courseId))
                .map(cf -> {
                    // Attempt to delete file from disk

                    try {
                        String filename = cf.getFileUrl().replaceFirst("/uploads/", "");
                        Path filePath = Paths.get(uploadDir).resolve(filename).toAbsolutePath();
                        Files.deleteIfExists(filePath);
                    } catch (IOException e) {
                    }

                    // Delete metadata
                    courseFileRepository.delete(cf);
                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "File deleted successfully"
                    ));
                })
                .orElse(ResponseEntity.status(404)
                        .body(Map.of("success", false, "message", "File not found for this course")));
    }
}

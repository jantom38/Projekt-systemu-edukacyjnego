package org.example;

import lombok.extern.slf4j.Slf4j;
import org.example.DataBaseRepositories.CourseFileRepository;
import org.example.DataBaseRepositories.CourseRepository;
import org.example.database.Course;
import org.example.database.CourseFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/courses/{courseId}/files")
public class FileUploadController {

    @Value("${file.upload-dir:#{systemProperties['user.dir'] + '/uploads'}}")
    private String uploadDir;

    @Autowired
    private CourseFileRepository courseFileRepository;

    @Autowired
    private CourseRepository courseRepository;

    @PostMapping("/upload")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<?> uploadFile(
            @PathVariable Long courseId,
            @RequestParam("file") MultipartFile file) {

        try {
            // 1. Walidacja kursu
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new RuntimeException("Course not found"));

            // 2. Walidacja pliku
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "File is empty"));
            }

            // 3. Przygotowanie katalogu
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath();
            log.info("Upload path: {}", uploadPath);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Created upload directory");
            }

            if (!Files.isWritable(uploadPath)) {
                log.error("Upload directory is not writable");
                return ResponseEntity.internalServerError()
                        .body(Map.of("error", "Upload directory not writable"));
            }

            // 4. Generowanie unikalnej nazwy pliku
            String originalFileName = file.getOriginalFilename();
            String fileExtension = originalFileName.contains(".")
                    ? originalFileName.substring(originalFileName.lastIndexOf("."))
                    : "";
            String uniqueFileName = UUID.randomUUID() + fileExtension;

            // 5. Zapis pliku na dysku
            Path filePath = uploadPath.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("File saved to: {}", filePath);

            // 6. Zapis metadanych w bazie
            CourseFile courseFile = new CourseFile();
            courseFile.setFileName(originalFileName);
            courseFile.setFileUrl("/" + uploadDir + "/" + uniqueFileName);
            courseFile.setCourse(course);

            CourseFile savedFile = courseFileRepository.save(courseFile);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "File uploaded successfully",
                    "filePath", filePath.toString(),
                    "fileUrl", savedFile.getFileUrl()
            ));

        } catch (Exception e) {
            log.error("File upload failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "File upload failed: " + e.getMessage()));
        }
    }
}
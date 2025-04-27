package org.example;

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


        log.info("Starting file upload for course ID: {}", courseId);

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
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // 4. Poprawione generowanie nazwy pliku
            String originalFileName = file.getOriginalFilename();
            String fileExtension = "";

            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }

            String uniqueFileName = UUID.randomUUID() + fileExtension;
            log.info("Generated filename: {}", uniqueFileName);

            // 5. Zapis pliku na dysku
            Path filePath = uploadPath.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("File saved to: {}", filePath);

            // 6. Zapis metadanych w bazie
            CourseFile courseFile = new CourseFile();
            courseFile.setFileName(originalFileName);
            courseFile.setFileUrl("/uploads/" + uniqueFileName); // Upewnij się że to pasuje do Twojej konfiguracji
            courseFile.setCourse(course);

            CourseFile savedFile = courseFileRepository.save(courseFile);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "File uploaded successfully",
                    "fileId", savedFile.getId(),
                    "fileName", savedFile.getFileName(),
                    "fileUrl", savedFile.getFileUrl(),
                    "filePath", filePath.toString()
            ));

        } catch (Exception e) {
            log.error("File upload failed", e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "message", "File upload failed: " + e.getMessage()));
        }
    }
}
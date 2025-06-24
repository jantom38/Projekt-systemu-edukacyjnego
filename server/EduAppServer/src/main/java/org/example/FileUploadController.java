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

/**
 * Kontroler odpowiedzialny za obsługę operacji przesyłania plików na serwer.
 * Umożliwia nauczycielom i administratorom przesyłanie plików do konkretnych kursów.
 */
@Slf4j
@RestController
@RequestMapping("/api/courses/{courseId}/files")
public class FileUploadController {

    /**
     * Ścieżka do katalogu, gdzie przechowywane są przesyłane pliki.
     * Wartość domyślna to "uploads" w katalogu użytkownika, ale może być nadpisana
     * przez właściwość `file.upload-dir` w `application.properties`.
     */
    @Value("${file.upload-dir:#{systemProperties['user.dir'] + '/uploads'}}")
    private String uploadDir;

    /**
     * Repozytorium do zarządzania encjami CourseFile w bazie danych.
     */
    @Autowired
    private CourseFileRepository courseFileRepository;

    /**
     * Repozytorium do zarządzania encjami Course w bazie danych.
     */
    @Autowired
    private CourseRepository courseRepository;

    /**
     * Obsługuje żądania przesyłania plików dla danego kursu.
     * Plik jest zapisywany na dysku, a jego metadane są zapisywane w bazie danych.
     * Wymaga roli TEACHER lub ADMIN.
     *
     * @param courseId Identyfikator kursu, do którego plik ma zostać przypisany.
     * @param file Przesyłany plik typu MultipartFile.
     * @return ResponseEntity zawierający status operacji i dane przesłanego pliku,
     * lub status błędu w przypadku niepowodzenia.
     */
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<?> uploadFile(@PathVariable Long courseId,
                                        @RequestParam("file") MultipartFile file) {
        try {
            // 1. Sprawdzenie, czy katalog istnieje i ewentualne utworzenie
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath); // Tworzy katalogi, jeśli nie istnieją
            log.info("Upload directory ensured: {}", uploadPath);

            // 2. Pobranie kursu
            Course course = courseRepository.findById(courseId)
                    .orElseThrow(() -> new RuntimeException("Course not found with ID: " + courseId));
            log.info("Found course: {}", course.getCourseName());

            // 3. Sprawdzenie, czy plik jest pusty
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("success", false, "message", "File is empty"));
            }
            log.info("Received file: {}", file.getOriginalFilename());

            // 4. Generowanie unikalnej nazwy pliku
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
            courseFile.setFileUrl("/files/" + uniqueFileName); // Upewnij się że to pasuje do Twojej konfiguracji
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
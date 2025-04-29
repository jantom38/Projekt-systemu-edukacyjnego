package org.example.controllers;

import org.example.database.Course;
import org.example.database.User;
import org.example.DataBaseRepositories.CourseRepository;
import org.example.DataBaseRepositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    // Inicjalizacja loggera
    private static final Logger LOGGER = Logger.getLogger(EnrollmentController.class.getName());

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @PostMapping("/enroll/{courseId}")
    public ResponseEntity<String> enrollInCourse(@PathVariable Long courseId, @RequestBody String accessKey) {
        // Logowanie początku operacji
        LOGGER.info("Próba zapisu na kurs o ID: " + courseId);

        // Pobranie nazwy zalogowanego użytkownika
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        LOGGER.info("Użytkownik: " + username);

        // Pobranie użytkownika z bazy danych
        User student = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Użytkownik nie znaleziony"));
        LOGGER.info("Znaleziono użytkownika: " + student.getUsername());

        // Pobranie kursu z bazy danych
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Kurs nie znaleziony"));
        LOGGER.info("Znaleziono kurs: " + course.getCourseName());

        // Sprawdzenie klucza dostępu
        if (!course.getAccessKey().equals(accessKey)) {
            LOGGER.warning("Nieprawidłowy klucz dostępu dla kursu: " + course.getCourseName());
            return ResponseEntity.badRequest().body("Nieprawidłowy klucz dostępu");
        }

        // Sprawdzenie, czy użytkownik jest już zapisany
        if (student.getEnrolledCourses().contains(course)) {
            LOGGER.warning("Użytkownik " + student.getUsername() + " jest już zapisany na kurs: " + course.getCourseName());
            return ResponseEntity.badRequest().body("Już zapisany na ten kurs");
        }

        // Zapisanie użytkownika na kurs
        student.getEnrolledCourses().add(course);
        userRepository.save(student);
        LOGGER.info("Użytkownik " + student.getUsername() + " zapisany na kurs: " + course.getCourseName());

        return ResponseEntity.ok("Zapisano na kurs");
    }
}
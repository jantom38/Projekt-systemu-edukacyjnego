package org.example.controllers;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.example.DataBaseRepositories.*;
import org.example.database.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/course-groups") // Dedykowany mapping dla grup
public class CourseGroupController {

    private final CourseGroupRepository courseGroupRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final UserCourseRepository userCourseRepository;

    @Autowired
    public CourseGroupController(CourseGroupRepository courseGroupRepository, CourseRepository courseRepository, UserRepository userRepository, UserCourseRepository userCourseRepository) {
        this.courseGroupRepository = courseGroupRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.userCourseRepository = userCourseRepository;
    }

    // Endpoint dla studentów i nauczycieli - pobiera listę wszystkich grup
    @GetMapping
    public ResponseEntity<List<CourseGroup>> getAllCourseGroups() {
        return ResponseEntity.ok(courseGroupRepository.findAll());
    }

    // =================================================================================
    // =========== NOWY, KLUCZOWY ENDPOINT DO ZAPISU NA KURS PRZEZ STUDENTA =============
    // =================================================================================
    @PostMapping("/{groupId}/enroll")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> enrollInCourse(@PathVariable Long groupId, @RequestBody Map<String, String> request) {
        String accessKey = request.get("accessKey");
        if (accessKey == null || accessKey.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Klucz dostępu jest wymagany."));
        }

        // 1. Znajdź grupę kursów
        Optional<CourseGroup> groupOpt = courseGroupRepository.findById(groupId);
        if (groupOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Grupa kursów nie znaleziona."));
        }

        // 2. Znajdź kurs w tej grupie z pasującym kluczem dostępu
        Optional<Course> courseOpt = courseRepository.findByCourseGroupIdAndAccessKey(groupId, accessKey);
        if (courseOpt.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Nieprawidłowy klucz dostępu dla tej grupy kursów."));
        }

        Course course = courseOpt.get();
        User student = Utils.getCurrentUser(userRepository);

        // 3. Sprawdź, czy użytkownik nie jest już zapisany
        if (userCourseRepository.existsByUserIdAndCourseId(student.getId(), course.getId())) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Jesteś już zapisany na ten kurs."));
        }

        // 4. Zapisz użytkownika na kurs
        UserCourse userCourse = new UserCourse(student, course);
        userCourseRepository.save(userCourse);
        log.info("Student '{}' zapisał się na kurs '{}' (ID: {}) z kluczem '{}'", student.getUsername(), course.getCourseName(), course.getId(), accessKey);

        return ResponseEntity.ok(Map.of("success", true, "message", "Zostałeś pomyślnie zapisany na kurs."));
    }


    // Endpointy dla nauczyciela/admina
    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<?> createCourseGroup(@RequestBody Map<String, String> request) {
        User teacher = Utils.getCurrentUser(userRepository);
        CourseGroup courseGroup = new CourseGroup();
        courseGroup.setName(request.get("name"));
        courseGroup.setDescription(request.get("description"));
        courseGroup.setTeacher(teacher);
        CourseGroup savedGroup = courseGroupRepository.save(courseGroup);
        return ResponseEntity.ok(savedGroup);
    }

    @PostMapping("/{groupId}/courses/{courseId}/duplicate")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<?> duplicateCourse(@PathVariable Long groupId, @PathVariable Long courseId, @RequestBody Map<String, String> request) {
        String newCourseName = request.get("newCourseName");
        String newAccessKey = request.get("newAccessKey");

        if (newCourseName == null || newCourseName.isBlank() || newAccessKey == null || newAccessKey.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Nowa nazwa kursu i klucz dostępu są wymagane."));
        }

        CourseGroup courseGroup = courseGroupRepository.findById(groupId).orElse(null);
        Course originalCourse = courseRepository.findById(courseId).orElse(null);
        if (courseGroup == null || originalCourse == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Nie znaleziono grupy lub kursu."));
        }

        // Prosta walidacja uprawnień
        String currentUsername = Utils.currentUsername();
        if (!originalCourse.getTeacher().getUsername().equals(currentUsername)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Brak uprawnień."));
        }

        Course newCourse = new Course();
        newCourse.setCourseName(newCourseName);
        newCourse.setDescription(originalCourse.getDescription());
        newCourse.setAccessKey(newAccessKey);
        newCourse.setTeacher(originalCourse.getTeacher());
        newCourse.setCourseGroup(courseGroup);

        courseRepository.save(newCourse);
        return ResponseEntity.ok(Map.of("success", true, "message", "Kurs zduplikowany pomyślnie."));
    }
    @DeleteMapping("/{groupId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Transactional // Ważne dla spójności operacji na wielu obiektach
    public ResponseEntity<?> deleteCourseGroup(@PathVariable Long groupId) {
        // Znajdź grupę do usunięcia
        CourseGroup groupToDelete = courseGroupRepository.findById(groupId)
                .orElse(null);

        if (groupToDelete == null) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Grupa kursów nie znaleziona."));
        }

        // Sprawdź uprawnienia - tylko właściciel grupy lub admin może ją usunąć
        String currentUsername = Utils.currentUsername();
        if (Utils.isTeacher(Utils.getAuthentication()) && !groupToDelete.getTeacher().getUsername().equals(currentUsername)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Brak uprawnień do usunięcia tej grupy."));
        }

        // "Osieracanie" kursów - usuwamy ich powiązanie z grupą
        for (Course course : groupToDelete.getCourses()) {
            course.setCourseGroup(null);
            courseRepository.save(course); // Zapisz zmiany w kursie
        }

        // Usuń samą, już pustą grupę
        courseGroupRepository.delete(groupToDelete);
        log.info("Użytkownik '{}' usunął grupę kursów '{}' (ID: {})", currentUsername, groupToDelete.getName(), groupId);

        return ResponseEntity.ok(Map.of("success", true, "message", "Grupa kursów została pomyślnie usunięta."));
    }
}

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
import java.util.stream.Collectors;

/**
 * @brief Kontroler odpowiedzialny za zarządzanie grupami kursów w systemie.
 *
 * Udostępnia endpointy do tworzenia, usuwania i zarządzania grupami kursów,
 * zapisywania studentów na kursy poprzez grupy oraz duplikowania kursów w obrębie grup.
 */
@Slf4j
@RestController
@RequestMapping("/api/course-groups")
public class CourseGroupController {

    private final CourseGroupRepository courseGroupRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final UserCourseRepository userCourseRepository;
    private final QuizQuestionRepository quizQuestionRepository;
    private final QuizRepository quizRepository;
    private final CourseFileRepository courseFileRepository;

    /**
     * @brief Konstruktor klasy CourseGroupController.
     * @param courseGroupRepository Repozytorium grup kursów.
     * @param courseRepository Repozytorium kursów.
     * @param userRepository Repozytorium użytkowników.
     * @param userCourseRepository Repozytorium powiązań użytkowników z kursami.
     * @param quizQuestionRepository Repozytorium pytań quizowych.
     * @param quizRepository Repozytorium quizów.
     * @param courseFileRepository Repozytorium plików kursów.
     */
    @Autowired
    public CourseGroupController(CourseGroupRepository courseGroupRepository,
                                 CourseRepository courseRepository,
                                 UserRepository userRepository,
                                 UserCourseRepository userCourseRepository,
                                 QuizQuestionRepository quizQuestionRepository,
                                 QuizRepository quizRepository,
                                 CourseFileRepository courseFileRepository) {
        this.courseGroupRepository = courseGroupRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.userCourseRepository = userCourseRepository;
        this.quizQuestionRepository = quizQuestionRepository;
        this.quizRepository = quizRepository;
        this.courseFileRepository = courseFileRepository;
    }

    /**
     * @brief Pobiera listę wszystkich grup kursów.
     * @return ResponseEntity z listą grup kursów w zależności od roli użytkownika:
     * - Admin widzi wszystkie grupy
     * - Nauczyciel widzi tylko swoje grupy
     * - Student widzi wszystkie grupy
     */
    @GetMapping
    public ResponseEntity<List<CourseGroup>> getAllCourseGroups() {
        Authentication auth = Utils.getAuthentication();
        String username = Utils.currentUsername();

        if (Utils.isAdmin(auth)) {
            return ResponseEntity.ok(courseGroupRepository.findAll());
        } else if (Utils.isTeacher(auth)) {
            return ResponseEntity.ok(courseGroupRepository.findByTeacherUsername(username));
        } else {
            return ResponseEntity.ok(courseGroupRepository.findAll());
        }
    }

    /**
     * @brief Zapisuje studenta na kurs w ramach grupy kursów.
     * @param groupId ID grupy kursów.
     * @param request Mapa zawierająca klucz dostępu:
     * - accessKey (String) - klucz dostępu do kursu
     * @return ResponseEntity z wynikiem operacji:
     * - success (boolean) - czy operacja się powiodła
     * - message (String) - komunikat o wyniku
     */
    @PostMapping("/{groupId}/enroll")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<?> enrollInCourse(@PathVariable Long groupId, @RequestBody Map<String, String> request) {
        String accessKey = request.get("accessKey");
        if (accessKey == null || accessKey.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Klucz dostępu jest wymagany."));
        }

        Optional<CourseGroup> groupOpt = courseGroupRepository.findById(groupId);
        if (groupOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("success", false, "message", "Grupa kursów nie znaleziona."));
        }

        Optional<Course> courseOpt = courseRepository.findByCourseGroupIdAndAccessKey(groupId, accessKey);
        if (courseOpt.isEmpty()) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Nieprawidłowy klucz dostępu dla tej grupy kursów."));
        }

        Course course = courseOpt.get();
        User student = Utils.getCurrentUser(userRepository);

        if (userCourseRepository.existsByUserIdAndCourseId(student.getId(), course.getId())) {
            return ResponseEntity.ok(Map.of("success", true, "message", "Jesteś już zapisany na ten kurs."));
        }

        UserCourse userCourse = new UserCourse(student, course);
        userCourseRepository.save(userCourse);
        log.info("Student '{}' zapisał się na kurs '{}' (ID: {}) z kluczem '{}'", student.getUsername(), course.getCourseName(), course.getId(), accessKey);

        return ResponseEntity.ok(Map.of("success", true, "message", "Zostałeś pomyślnie zapisany na kurs."));
    }

    /**
     * @brief Tworzy nową grupę kursów.
     * @param request Mapa zawierająca dane grupy:
     * - name (String) - nazwa grupy
     * - description (String) - opis grupy
     * @return ResponseEntity z wynikiem operacji:
     * - success (boolean) - czy operacja się powiodła
     * - message (String) - komunikat o wyniku
     * - courseGroup (CourseGroup) - utworzona grupa kursów
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<?> createCourseGroup(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String description = request.get("description");

        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Nazwa grupy jest wymagana"));
        }

        User teacher = Utils.getCurrentUser(userRepository);
        CourseGroup courseGroup = new CourseGroup();
        courseGroup.setName(name);
        courseGroup.setDescription(description);
        courseGroup.setTeacher(teacher);

        CourseGroup savedGroup = courseGroupRepository.save(courseGroup);
        log.info("Utworzono nową grupę kursów '{}' (ID: {}) przez użytkownika '{}'", name, savedGroup.getId(), teacher.getUsername());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Grupa kursów utworzona pomyślnie",
                "courseGroup", savedGroup
        ));
    }

    /**
     * @brief Duplikuje kurs w obrębie grupy kursów.
     * @param groupId ID grupy kursów.
     * @param courseId ID kursu do zduplikowania.
     * @param request Mapa zawierająca dane nowego kursu:
     * - newCourseName (String) - nazwa nowego kursu
     * - newAccessKey (String) - nowy klucz dostępu
     * @return ResponseEntity z wynikiem operacji:
     * - success (boolean) - czy operacja się powiodła
     * - message (String) - komunikat o wyniku
     * - course (Course) - zduplikowany kurs
     */
    @PostMapping("/course-groups/{groupId}/courses/{courseId}/duplicate")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Transactional
    public ResponseEntity<?> duplicateCourse(@PathVariable Long groupId,
                                             @PathVariable Long courseId,
                                             @RequestBody Map<String, String> request) {
        String newCourseName = request.get("newCourseName");
        String newAccessKey = request.get("newAccessKey");

        if (newCourseName == null || newCourseName.isBlank() || newAccessKey == null || newAccessKey.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "Nazwa kursu i klucz dostępu są wymagane"));
        }

        Course originalCourse = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Oryginalny kurs o ID: " + courseId + " nie istnieje."));
        CourseGroup group = courseGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Grupa kursów o ID: " + groupId + " nie istnieje."));

        Authentication auth = Utils.getAuthentication();
        if (Utils.isTeacher(auth) && !originalCourse.getTeacher().getUsername().equals(Utils.currentUsername())) {
            return ResponseEntity.status(403)
                    .body(Map.of("success", false, "message", "Brak uprawnień do duplikowania tego kursu"));
        }

        Course newCourse = new Course();
        newCourse.setCourseName(newCourseName);
        newCourse.setAccessKey(newAccessKey);
        newCourse.setDescription(originalCourse.getDescription());
        newCourse.setTeacher(originalCourse.getTeacher());
        newCourse.setCourseGroup(group);
        Course savedNewCourse = courseRepository.save(newCourse);
        log.info("Utworzono nowy kurs '{}' (ID: {}) na podstawie kursu ID: {}", newCourse.getCourseName(), savedNewCourse.getId(), originalCourse.getId());

        List<Quiz> originalQuizzes = quizRepository.findByCourseId(originalCourse.getId());
        for (Quiz originalQuiz : originalQuizzes) {
            Quiz newQuiz = new Quiz();
            newQuiz.setTitle(originalQuiz.getTitle());
            newQuiz.setDescription(originalQuiz.getDescription());
            newQuiz.setCourse(savedNewCourse);
            newQuiz.setNumberOfQuestionsToDisplay(originalQuiz.getNumberOfQuestionsToDisplay());

            List<QuizQuestion> newQuestions = originalQuiz.getQuestions().stream().map(originalQuestion -> {
                QuizQuestion newQuestion = new QuizQuestion();
                newQuestion.setQuestionText(originalQuestion.getQuestionText());
                newQuestion.setQuestionType(originalQuestion.getQuestionType());
                newQuestion.setOptions(originalQuestion.getOptions());
                newQuestion.setCorrectAnswer(originalQuestion.getCorrectAnswer());
                newQuestion.setQuiz(newQuiz);
                return newQuestion;
            }).collect(Collectors.toList());

            newQuiz.setQuestions(newQuestions);
            quizRepository.save(newQuiz);
        }
        log.info("Skopiowano {} quizów dla nowego kursu ID: {}", originalQuizzes.size(), savedNewCourse.getId());

        List<CourseFile> originalFiles = courseFileRepository.findByCourseId(originalCourse.getId());
        for (CourseFile originalFile : originalFiles) {
            CourseFile newFileLink = new CourseFile();
            newFileLink.setFileName(originalFile.getFileName());
            newFileLink.setFileUrl(originalFile.getFileUrl());
            newFileLink.setCourse(savedNewCourse);
            courseFileRepository.save(newFileLink);
        }
        log.info("Utworzono {} powiązań do istniejących plików dla nowego kursu ID: {}", originalFiles.size(), savedNewCourse.getId());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Kurs, quizy i pliki zostały pomyślnie zduplikowane",
                "course", savedNewCourse
        ));
    }

    /**
     * @brief Usuwa grupę kursów.
     * @param groupId ID grupy do usunięcia.
     * @return ResponseEntity z wynikiem operacji:
     * - success (boolean) - czy operacja się powiodła
     * - message (String) - komunikat o wyniku
     */
    @DeleteMapping("/{groupId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    @Transactional
    public ResponseEntity<?> deleteCourseGroup(@PathVariable Long groupId) {
        CourseGroup groupToDelete = courseGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Grupa kursów o ID: " + groupId + " nie istnieje."));

        Authentication auth = Utils.getAuthentication();
        String currentUsername = Utils.currentUsername();
        if (Utils.isTeacher(auth) && !groupToDelete.getTeacher().getUsername().equals(currentUsername)) {
            return ResponseEntity.status(403).body(Map.of("success", false, "message", "Brak uprawnień do usunięcia tej grupy."));
        }

        for (Course course : groupToDelete.getCourses()) {
            course.setCourseGroup(null);
            courseRepository.save(course);
        }

        courseGroupRepository.delete(groupToDelete);
        log.info("Użytkownik '{}' usunął grupę kursów '{}' (ID: {})", currentUsername, groupToDelete.getName(), groupId);

        return ResponseEntity.ok(Map.of("success", true, "message", "Grupa kursów została pomyślnie usunięta."));
    }
}
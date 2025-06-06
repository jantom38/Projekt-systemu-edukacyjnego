package org.example.database;

import jakarta.annotation.PostConstruct;
import org.example.DataBaseRepositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    // DODANO REPOZYTORIUM DLA GRUP KURSÓW
    @Autowired
    private CourseGroupRepository courseGroupRepository;

    @Autowired
    private CourseFileRepository courseFileRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        // Inicjalizacja w odpowiedniej kolejności
        initUsers();
        initCourseGroupsAndCourses();
        initCourseFiles();
    }

    private void initUsers() {
        if (userRepository.findByUsername("user").isEmpty()) {
            User student = new User();
            student.setUsername("user");
            student.setPassword(passwordEncoder.encode("user"));
            student.setRole(UserRole.STUDENT);
            userRepository.save(student);
        }
        if (userRepository.findByUsername("user3").isEmpty()) {
            User admin = new User();
            admin.setUsername("user3");
            admin.setPassword(passwordEncoder.encode("user"));
            admin.setRole(UserRole.ADMIN);
            userRepository.save(admin);
        }
        if (userRepository.findByUsername("teacher").isEmpty()) {
            User teacher = new User();
            teacher.setUsername("teacher");
            teacher.setPassword(passwordEncoder.encode("teacher123"));
            teacher.setRole(UserRole.TEACHER);
            userRepository.save(teacher);
        }
        if (userRepository.findByUsername("teacher1").isEmpty()) {
            User teacher = new User();
            teacher.setUsername("teacher1");
            teacher.setPassword(passwordEncoder.encode("teacher123"));
            teacher.setRole(UserRole.TEACHER);
            userRepository.save(teacher);
        }
    }

    // ZASTĄPIONO STARE METODY JEDNĄ, POPRAWNĄ METODĄ
    private void initCourseGroupsAndCourses() {
        // Upewniamy się, że dane są dodawane tylko raz, sprawdzając repozytorium grup
        if (courseGroupRepository.count() == 0) {
            // Pobieramy nauczycieli
            User teacher = userRepository.findByUsername("teacher")
                    .orElseThrow(() -> new RuntimeException("Teacher 'teacher' not found"));
            User teacher1 = userRepository.findByUsername("teacher1")
                    .orElseThrow(() -> new RuntimeException("Teacher 'teacher1' not found"));

            // --- GRUPA 1: KURSY JAVY ---
            CourseGroup javaGroup = new CourseGroup();
            javaGroup.setName("Programowanie w Javie");
            javaGroup.setDescription("Grupa kursów poświęcona nauce języka Java na różnych poziomach zaawansowania.");
            javaGroup.setTeacher(teacher);
            courseGroupRepository.save(javaGroup); // Zapisz grupę, aby uzyskać ID

            // Kurs 1 w grupie Java
            Course course1 = new Course();
            course1.setCourseName("Java Basics - Edycja Jesień 2024");
            course1.setDescription("Wprowadzenie do programowania w języku Java.");
            course1.setAccessKey("JAVA-FALL24");
            course1.setTeacher(teacher);
            course1.setCourseGroup(javaGroup); // Przypisanie kursu do grupy
            courseRepository.save(course1);

            // Kurs 2 w grupie Java
            Course course2 = new Course();
            course2.setCourseName("Java Advanced - Edycja Wiosna 2025");
            course2.setDescription("Zaawansowane techniki i wzorce projektowe w Javie.");
            course2.setAccessKey("JAVA-SPR25");
            course2.setTeacher(teacher);
            course2.setCourseGroup(javaGroup); // Przypisanie kursu do tej samej grupy
            courseRepository.save(course2);


            // --- GRUPA 2: SZKOLENIA BHP ---
            CourseGroup bhpGroup = new CourseGroup();
            bhpGroup.setName("Szkolenia BHP");
            bhpGroup.setDescription("Okresowe szkolenia z Bezpieczeństwa i Higieny Pracy.");
            bhpGroup.setTeacher(teacher1);
            courseGroupRepository.save(bhpGroup);

            // Kurs 1 w grupie BHP
            Course courseBHP = new Course();
            courseBHP.setCourseName("BHP dla Pracowników Biurowych 2025");
            courseBHP.setDescription("Obowiązkowe szkolenie okresowe dla pracowników administracyjno-biurowych.");
            courseBHP.setAccessKey("BHP-ADM25");
            courseBHP.setTeacher(teacher1);
            courseBHP.setCourseGroup(bhpGroup);
            courseRepository.save(courseBHP);
        }
    }

    private void initCourseFiles() {
        if (courseFileRepository.count() == 0) {
            // ZAKTUALIZOWANO NAZWĘ KURSU, ABY PASOWAŁA DO NOWYCH DANYCH
            Course javaCourse = courseRepository.findByCourseName("Java Basics - Edycja Jesień 2024")
                    .orElseThrow(() -> new RuntimeException("Kurs 'Java Basics - Edycja Jesień 2024' nie znaleziony!"));

            courseFileRepository.save(new CourseFile("java_intro.pdf", "/files/java_intro.pdf", javaCourse));
            courseFileRepository.save(new CourseFile("java_syntax.pdf", "/files/java_syntax.pdf", javaCourse));
        }
    }
}
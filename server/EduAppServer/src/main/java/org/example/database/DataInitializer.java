package org.example.database;

import jakarta.annotation.PostConstruct;
import org.example.DataBaseRepositories.CourseFileRepository;
import org.example.DataBaseRepositories.CourseRepository;
import org.example.DataBaseRepositories.QuizQuestionRepository;
import org.example.DataBaseRepositories.QuizRepository;
import org.example.DataBaseRepositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CourseFileRepository courseFileRepository;

    @Autowired
    private QuizRepository quizRepository;

    @Autowired
    private QuizQuestionRepository quizQuestionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        initUsers();
        initCourses();
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
        if (userRepository.findByUsername("user2").isEmpty()) {
            User admin = new User();
            admin.setUsername("user2");
            admin.setPassword(passwordEncoder.encode("user"));
            admin.setRole(UserRole.STUDENT);
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

    private void initCourses() {
        if (courseRepository.count() == 0) {
            User teacher = userRepository.findByUsername("teacher")
                    .orElseThrow();
            Course course1 = new Course();
            course1.setCourseName("Java Basics");
            course1.setDescription("Introduction to Java programming");
            course1.setAccessKey("JAVA-101");
            course1.setTeacher(teacher);
            courseRepository.save(course1);
        }
    }

    private void initCourseFiles() {
        if (courseFileRepository.count() == 0) {
            Course javaCourse = courseRepository.findByCourseName("Java Basics")
                    .orElseThrow(() -> new RuntimeException("Kurs 'Java Basics' nie znaleziony!"));

            // Spójne nazwy plików (małe litery + podkreślenia)
            courseFileRepository.save(new CourseFile("java_intro.pdf", "/files/java_intro.pdf", javaCourse));
            courseFileRepository.save(new CourseFile("java_exercises.zip", "/files/java_exercises.zip", javaCourse));
        }
    }

        }
// pliki do kursu nauczycieli
//INSERT INTO course_file (file_name, file_url, course_id) VALUES
//('I. Regulacje prawne.doc', '/files/I. Regulacje prawne.doc', 2),
//        ('II. Ocena zagrożeń.doc', '/files/II. Ocena zagrożeń.doc', 2),
//        ('III. Organ. stanowisk pracy.doc', '/files/III. Organ. stanowisk pracy.doc', 2),
//        ('IV. Wypadki, choroby zaw..doc', '/files/IV. Wypadki, choroby zaw..doc', 2),
//        ('V. Ochrona ppoż..doc', '/files/V. Ochrona ppoż..doc', 2),
//        ('AED + BHP2.docx', '/files/AED + BHP2.docx', 2),
//        ('Ma. Charakterystyczne wypadki w PŚk..docx', '/files/Ma. Charakterystyczne wypadki w PŚk..docx', 2),
//        ('System udzielania I pomocy w PŚk.docx', '/files/System udzielania I pomocy w PŚk.docx', 2);
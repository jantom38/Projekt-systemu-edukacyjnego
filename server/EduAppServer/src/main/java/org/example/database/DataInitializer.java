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
        initQuizzes();
    }

    private void initUsers() {
        if (userRepository.findByUsername("user").isEmpty()) {
            User admin = new User();
            admin.setUsername("user");
            admin.setPassword(passwordEncoder.encode("password"));
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

    private void initQuizzes() {
        if (quizRepository.count() == 0) {
            Course javaCourse = courseRepository.findByCourseName("Java Basics")
                    .orElseThrow(() -> new RuntimeException("Kurs 'Java Basics' nie znaleziony!"));

            Quiz quiz1 = new Quiz("Java Fundamentals Quiz", "Test your knowledge of Java basics", javaCourse);
            quizRepository.save(quiz1);

            // Sample questions for the quiz
            quizQuestionRepository.save(new QuizQuestion(
                "What is the correct syntax for the main method in Java?",
                "public static void main(String[] args)",
                "public void main(String[] args)",
                "public static void main(String args)",
                "public static void main(String[] args)",
                "static void main(String[] args)",
                quiz1
            ));

            quizQuestionRepository.save(new QuizQuestion(
                "Which keyword is used to inherit a class in Java?",
                "extends",
                "implements",
                "extends",
                "super",
                "this",
                quiz1
            ));
        }
    }
}
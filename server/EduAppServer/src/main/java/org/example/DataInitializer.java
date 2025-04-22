package org.example;

import jakarta.annotation.PostConstruct;
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
    private PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        initUsers();
        initCourses();
        initCourseFiles();
    }

    private void initUsers() {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("password123"));
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
    }

    private void initCourses() {
        if (courseRepository.count() == 0) {
            Course course1 = new Course();
            course1.setCourseName("Java Basics");
            course1.setDescription("Introduction to Java programming");
            course1.setAccessKey("JAVA-101");
            courseRepository.save(course1);

            Course course2 = new Course();
            course2.setCourseName("Spring Boot");
            course2.setDescription("Building web applications with Spring");
            course2.setAccessKey("SPRING-202");
            courseRepository.save(course2);

            Course course3 = new Course();
            course3.setCourseName("Hibernate");
            course3.setDescription("Database access with Hibernate");
            course3.setAccessKey("HIB-303");
            courseRepository.save(course3);
        }
    }

    private void initCourseFiles() {
        if (courseFileRepository.count() == 0) {
            Course javaCourse = courseRepository.findByCourseName("Java Basics")
                    .orElseThrow(() -> new RuntimeException("Kurs 'Java Basics' nie znaleziony!"));
            Course springCourse = courseRepository.findByCourseName("Spring Boot")
                    .orElseThrow(() -> new RuntimeException("Kurs 'Spring Boot' nie znaleziony!"));
            Course hibernateCourse = courseRepository.findByCourseName("Hibernate")
                    .orElseThrow(() -> new RuntimeException("Kurs 'Hibernate' nie znaleziony!"));

            // Spójne nazwy plików (małe litery + podkreślenia)
            courseFileRepository.save(new CourseFile("java_intro.pdf", "http://10.0.2.2:8080/files/java_intro.pdf", javaCourse));
            courseFileRepository.save(new CourseFile("java_exercises.zip", "http://10.0.2.2:8080/files/java_exercises.zip", javaCourse));

            courseFileRepository.save(new CourseFile("spring_guide.pdf", "http://localhost:8080/files/spring_guide.pdf", springCourse));
            courseFileRepository.save(new CourseFile("spring_tutorial.mp4", "http://localhost:8080/files/spring_tutorial.mp4", springCourse));

            courseFileRepository.save(new CourseFile("hibernate_docs.pdf", "http://localhost:8080/files/hibernate_docs.pdf", hibernateCourse));
        }
    }
}
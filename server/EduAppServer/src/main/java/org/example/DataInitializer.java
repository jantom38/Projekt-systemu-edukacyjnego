package org.example;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @PostConstruct
    public void init() {
        initUsers();
        initCourses();
    }

    private void initUsers() {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User user = new User();
            user.setUsername("admin");
            user.setPassword("password123");
            userRepository.save(user);
        }
    }

    private void initCourses() {
        if (courseRepository.count() == 0) { // Dodajemy kursy tylko jeśli tabela jest pusta
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
}
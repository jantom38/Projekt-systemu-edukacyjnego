package org.example;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class ListOfCoursesModule {

    @Autowired
    private CourseRepository courseRepository;

    @GetMapping
    public List<Course> getAllCourses() {
       // System.out.println("User roles: " + authentication.getAuthorities()); // Debug ról
        return courseRepository.findAll();
    }
}
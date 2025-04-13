package org.example;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;
//@RestController
//@RequestMapping("/api/courses")
// W Twoim ListOfCoursesModule.java
@RestController
@RequestMapping("/api/courses")
public class ListOfCoursesModule {

    @Autowired
    private CourseRepository courseRepository;

    @GetMapping
    public List<Course> getAllCourses() {
        return courseRepository.findAll();
    }
}
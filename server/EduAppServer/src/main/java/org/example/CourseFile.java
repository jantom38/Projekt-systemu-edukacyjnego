package org.example;

import jakarta.persistence.*;

@Entity
public class CourseFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String fileUrl;

    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    // Gettery, settery, konstruktory
    public CourseFile() {}
    public CourseFile(String fileName, String fileUrl, Course course) {
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.course = course;
    }
    // ... gettery i settery
}
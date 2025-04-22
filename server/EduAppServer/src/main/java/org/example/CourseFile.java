package org.example;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @JsonIgnore
    private Course course;

    // Konstruktory
    public CourseFile() {}
    public CourseFile(String fileName, String fileUrl, Course course) {
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.course = course;
    }

    // Gettery i settery
    public Long getId() { return id; }
    public String getFileName() { return fileName; }
    public String getFileUrl() { return fileUrl; }
    public Course getCourse() { return course; }
    // ... settery ...
    public void setId(Long id) { this.id = id; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public void setCourse(Course course) { this.course = course; }
}
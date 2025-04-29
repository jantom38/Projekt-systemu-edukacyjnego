package org.example.database;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

@Entity
@Table(name = "Courses")
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String courseName; // Zmiana na camelCase
    private String description;
    private String accessKey; // Zmiana na camelCase
@ManyToOne(fetch=FetchType.LAZY)
@JoinColumn(name = "teacher_id", nullable = false)
@JsonIgnore
private User teacher;
    // Gettery i settery
    public Long getId() { return id; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
    public User getTeacher() {return teacher;}
    public void setTeacher(User teacher) {this.teacher = teacher;}
}
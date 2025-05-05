package org.example.database;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_courses")
public class UserCourse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    // Dodatkowe pola (opcjonalne)
    private LocalDateTime joinedAt = LocalDateTime.now();
    private boolean active = true;

    // Konstruktory
    public UserCourse() {}

    public UserCourse(User user, Course course) {
        this.user = user;
        this.course = course;
    }

    // Gettery i settery
    public Long getId() { return id; }
    public User getUser() { return user; }
    public Course getCourse() { return course; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public boolean isActive() { return active; }

    public void setUser(User user) { this.user = user; }
    public void setCourse(Course course) { this.course = course; }
    public void setActive(boolean active) { this.active = active; }
}
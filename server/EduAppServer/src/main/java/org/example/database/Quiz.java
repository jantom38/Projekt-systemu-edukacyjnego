package org.example.database;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.val;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quizzes")
public class Quiz {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnore
    private Course course;

    private LocalDateTime createdAt = LocalDateTime.now();

    // New field for the number of questions to display
    @Column(nullable = false)
    private int numberOfQuestionsToDisplay;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<QuizQuestion> questions = new ArrayList<>();
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<QuizResult> quizResults = new ArrayList<>();
    // Constructors
    public Quiz() {}

    public Quiz(String title, String description, Course course, int numberOfQuestionsToDisplay) {
        this.title = title;
        this.description = description;
        this.course = course;
        this.numberOfQuestionsToDisplay = numberOfQuestionsToDisplay;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<QuizQuestion> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuizQuestion> questions) {
        this.questions = questions;
    }

    // New getter and setter for numberOfQuestionsToDisplay
    public int getNumberOfQuestionsToDisplay() {
        return numberOfQuestionsToDisplay;
    }

    public void setNumberOfQuestionsToDisplay(int numberOfQuestionsToDisplay) {
        this.numberOfQuestionsToDisplay = numberOfQuestionsToDisplay;
    }
}
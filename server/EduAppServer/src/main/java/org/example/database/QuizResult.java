// QuizResult.java
package org.example.database;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "quiz_results")
public class QuizResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    @Column(nullable = false)
    private int correctAnswers;

    @Column(nullable = false)
    private int totalQuestions;

    @Column(nullable = false)
    private LocalDateTime completionDate;

    @OneToMany(mappedBy = "quizResult", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<QuizAnswer> quizAnswers;

    // Konstruktory
    public QuizResult() {}

    public QuizResult(User user, Quiz quiz, int correctAnswers, int totalQuestions, LocalDateTime completionDate) {
        this.user = user;
        this.quiz = quiz;
        this.correctAnswers = correctAnswers;
        this.totalQuestions = totalQuestions;
        this.completionDate = completionDate;
    }

    // Gettery i Settery
    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Quiz getQuiz() { return quiz; }
    public void setQuiz(Quiz quiz) { this.quiz = quiz; }
    public int getCorrectAnswers() { return correctAnswers; }
    public void setCorrectAnswers(int correctAnswers) { this.correctAnswers = correctAnswers; }
    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
    public LocalDateTime getCompletionDate() { return completionDate; }
    public void setCompletionDate(LocalDateTime completionDate) { this.completionDate = completionDate; }
    public List<QuizAnswer> getQuizAnswers() { return quizAnswers; }
    public void setQuizAnswers(List<QuizAnswer> quizAnswers) { this.quizAnswers = quizAnswers; }
}
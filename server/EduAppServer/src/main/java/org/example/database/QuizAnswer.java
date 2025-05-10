package org.example.database;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "quiz_answers")
@Data
public class QuizAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_result_id", nullable = false)
    private QuizResult quizResult;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    @Column(nullable = false)
    private String userAnswer;

    @Column(nullable = false)
    private boolean isCorrect;
    public void setIsCorrect(boolean isCorrect) {
        this.isCorrect = isCorrect;
    }
    public boolean isCorrect() {
        return isCorrect;
    }
}
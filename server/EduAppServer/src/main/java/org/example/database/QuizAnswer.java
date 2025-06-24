package org.example.database;

import jakarta.persistence.*;
import lombok.Data;

/**
 * @brief
 * Klasa encji reprezentująca odpowiedź użytkownika na pytanie w quizie.
 * Mapowana jest do tabeli "quiz_answers" w bazie danych.
 */
@Entity
@Table(name = "quiz_answers")
@Data
public class QuizAnswer {
    /**
     * Unikalny identyfikator odpowiedzi.
     * Jest to klucz główny generowany automatycznie.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Obiekt QuizResult, do którego należy ta odpowiedź.
     * Wiele odpowiedzi może być powiązanych z jednym wynikiem quizu.
     * Kolumna 'quiz_result_id' jest kluczem obcym i nie może być nullem.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_result_id", nullable = false)
    private QuizResult quizResult;

    /**
     * Obiekt QuizQuestion, na które udzielono odpowiedzi.
     * Wiele odpowiedzi może odnosić się do tego samego pytania.
     * Kolumna 'question_id' jest kluczem obcym i nie może być nullem.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuizQuestion question;

    /**
     * Odpowiedź udzielona przez użytkownika.
     * Kolumna nie może być nullem.
     */
    @Column(nullable = false)
    private String userAnswer;

    /**
     * Flaga wskazująca, czy odpowiedź użytkownika jest poprawna.
     * Kolumna nie może być nullem.
     */
    @Column(nullable = false)
    private boolean isCorrect;
    public void setIsCorrect(boolean isCorrect) {
        this.isCorrect = isCorrect;
    }
    public boolean isCorrect() {
        return isCorrect;
    }
}
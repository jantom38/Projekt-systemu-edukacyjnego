package org.example.database;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Klasa encji reprezentująca wynik quizu użytkownika.
 * Mapowana jest do tabeli "quiz_results" w bazie danych.
 */
@Entity
@Table(name = "quiz_results")
public class QuizResult {
    /**
     * Unikalny identyfikator wyniku quizu.
     * Jest to klucz główny generowany automatycznie.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Użytkownik, który przystąpił do quizu.
     * Wiele wyników quizów może być powiązanych z jednym użytkownikiem.
     * Kolumna 'user_id' jest kluczem obcym i nie może być nullem.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Quiz, którego dotyczy wynik.
     * Wiele wyników quizów może być powiązanych z jednym quizem.
     * Kolumna 'quiz_id' jest kluczem obcym i nie może być nullem.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    private Quiz quiz;

    /**
     * Liczba poprawnych odpowiedzi udzielonych przez użytkownika. Nie może być nullem.
     */
    @Column(nullable = false)
    private int correctAnswers;

    /**
     * Całkowita liczba pytań w quizie. Nie może być nullem.
     */
    @Column(nullable = false)
    private int totalQuestions;

    /**
     * Data i czas ukończenia quizu. Nie może być nullem.
     */
    @Column(nullable = false)
    private LocalDateTime completionDate;

    /**
     * Lista odpowiedzi udzielonych przez użytkownika w ramach tego wyniku quizu.
     * Relacja jeden do wielu z klasą QuizAnswer.
     * Operacje kaskadowe są włączone.
     */
    @OneToMany(mappedBy = "quizResult", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<QuizAnswer> quizAnswers;

    // Konstruktory
    /**
     * Domyślny konstruktor.
     */
    public QuizResult() {}

    /**
     * Konstruktor z parametrami do inicjalizacji wyniku quizu.
     * @param user Użytkownik, który przystąpił do quizu.
     * @param quiz Quiz, którego dotyczy wynik.
     * @param correctAnswers Liczba poprawnych odpowiedzi.
     * @param totalQuestions Całkowita liczba pytań.
     * @param completionDate Data i czas ukończenia quizu.
     */
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
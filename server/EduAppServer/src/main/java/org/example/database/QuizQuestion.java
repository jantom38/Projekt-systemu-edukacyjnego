package org.example.database;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.Map;

/**
 * Klasa encji reprezentująca pytanie w quizie.
 * Mapowana jest do tabeli "quiz_questions" w bazie danych.
 */
@Entity
@Table(name = "quiz_questions")
public class QuizQuestion {
    /**
     * Unikalny identyfikator pytania.
     * Jest to klucz główny generowany automatycznie.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Treść pytania. Nie może być nullem.
     */
    @Column(nullable = false)
    private String questionText;

    /**
     * Typ pytania, np. "multiple_choice" (wielokrotny wybór) lub "open_ended" (otwarte). Nie może być nullem.
     */
    @Column(nullable = false)
    private String questionType;

    /**
     * Mapa opcji dla pytań wielokrotnego wyboru.
     * Przykładowo: {"A": "opcja A", "B": "opcja B", ...}.
     * Przechowywana jako typ JSONB w bazie danych.
     */
    @Column(columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, String> options;

    /**
     * Poprawna odpowiedź. Dla pytań wielokrotnego wyboru jest to klucz poprawnej opcji,
     * dla pytań otwartych jest to oczekiwana odpowiedź tekstowa.
     */
    private String correctAnswer;

    /**
     * Obiekt Quiz, do którego należy to pytanie.
     * Wiele pytań może należeć do jednego quizu.
     * Kolumna 'quiz_id' jest kluczem obcym i nie może być nullem.
     * Pole jest ignorowane podczas serializacji JSON.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quiz_id", nullable = false)
    @JsonIgnore
    private Quiz quiz;

    // Konstruktory
    /**
     * Domyślny konstruktor.
     */
    public QuizQuestion() {}

    /**
     * Konstruktor z parametrami do inicjalizacji pytania quizu.
     * @param questionText Treść pytania.
     * @param questionType Typ pytania.
     * @param options Opcje odpowiedzi (dla pytań wielokrotnego wyboru).
     * @param correctAnswer Poprawna odpowiedź.
     * @param quiz Quiz, do którego należy pytanie.
     */
    public QuizQuestion(String questionText, String questionType, Map<String, String> options, String correctAnswer, Quiz quiz) {
        this.questionText = questionText;
        this.questionType = questionType;
        this.options = options;
        this.correctAnswer = correctAnswer;
        this.quiz = quiz;
    }

    // Gettery i Settery
    public Long getId() { return id; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }
    public Map<String, String> getOptions() { return options; }
    public void setOptions(Map<String, String> options) { this.options = options; }
    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
    public Quiz getQuiz() { return quiz; }
    public void setQuiz(Quiz quiz) { this.quiz = quiz; }
}
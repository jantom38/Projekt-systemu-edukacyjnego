package org.example.database;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.val;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * @brief
 * Klasa encji reprezentująca quiz.
 * Mapowana jest do tabeli "quizzes" w bazie danych.
 */
@Entity
@Table(name = "quizzes")
public class Quiz {
    /**
     * Unikalny identyfikator quizu.
     * Jest to klucz główny generowany automatycznie.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tytuł quizu. Nie może być nullem.
     */
    @Column(nullable = false)
    private String title;

    /**
     * Opis quizu.
     */
    private String description;

    /**
     * Kurs, do którego należy quiz.
     * Wiele quizów może być przypisanych do jednego kursu.
     * Kolumna 'course_id' jest kluczem obcym i nie może być nullem.
     * Pole jest ignorowane podczas serializacji JSON.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    @JsonIgnore
    private Course course;

    /**
     * Data i czas utworzenia quizu. Domyślnie ustawiane na aktualny czas.
     */
    private LocalDateTime createdAt = LocalDateTime.now();

    /**
     * Liczba pytań do wyświetlenia w quizie. Nie może być nullem.
     */
    @Column(nullable = false)
    private int numberOfQuestionsToDisplay;

    /**
     * Lista pytań należących do tego quizu.
     * Relacja jeden do wielu z klasą QuizQuestion.
     * Operacje kaskadowe są włączone.
     */
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<QuizQuestion> questions = new ArrayList<>();
    /**
     * Lista wyników quizów związanych z tym quizem.
     * Relacja jeden do wielu z klasą QuizResult.
     * Operacje kaskadowe (np. usunięcie quizu usuwa powiązane wyniki) są włączone.
     * Pole jest ignorowane podczas serializacji JSON.
     */
    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<QuizResult> quizResults = new ArrayList<>();
    // Constructors
    /**
     * Domyślny konstruktor.
     */
    public Quiz() {}

    /**
     * Konstruktor z parametrami do inicjalizacji quizu.
     * @param title Tytuł quizu.
     * @param description Opis quizu.
     * @param course Kurs, do którego quiz jest przypisany.
     * @param numberOfQuestionsToDisplay Liczba pytań do wyświetlenia.
     */
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
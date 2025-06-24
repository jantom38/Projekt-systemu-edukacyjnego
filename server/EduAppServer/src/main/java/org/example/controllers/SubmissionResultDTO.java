/**
 * @file SubmissionResultDTO.java
 * @brief Klasa DTO (Data Transfer Object) do przesyłania wyników przesłanego quizu.
 *
 * Służy do hermetyzacji danych dotyczących wyniku quizu, takich jak
 * status sukcesu, uzyskany wynik, liczba poprawnych odpowiedzi,
 * całkowita liczba pytań oraz wynik procentowy.
 */
package org.example.controllers;

import lombok.Data;

/**
 * @brief Klasa DTO reprezentująca wynik przesłania quizu.
 */
@Data
public class SubmissionResultDTO {
    private boolean success; ///< Informacja, czy przesłanie zakończyło się sukcesem.
    private String score; ///< Wynik w formacie "poprawne/wszystkie".
    private int correctAnswers; ///< Liczba poprawnych odpowiedzi.
    private int totalQuestions; ///< Całkowita liczba pytań w quizie.
    private double percentage; ///< Wynik procentowy.

    /**
     * @brief Konstruktor do tworzenia obiektu SubmissionResultDTO.
     * @param success true jeśli przesłanie było udane, false w przeciwnym razie.
     * @param score Wynik w formacie ciągu znaków (np. "5/10").
     * @param correctAnswers Liczba poprawnych odpowiedzi.
     * @param totalQuestions Całkowita liczba pytań.
     * @param percentage Wynik procentowy.
     */
    public SubmissionResultDTO(boolean success, String score, int correctAnswers, int totalQuestions, double percentage) {
        this.success = success;
        this.score = score;
        this.correctAnswers = correctAnswers;
        this.totalQuestions = totalQuestions;
        this.percentage = percentage;
    }
}
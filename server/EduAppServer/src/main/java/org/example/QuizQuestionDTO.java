// plik: src/main/java/org/example/dto/QuizQuestionDTO.java
package org.example.dto;

import java.util.Map;

/**
 * @brief
 * Rekord DTO (Data Transfer Object) reprezentujący pytanie quizu przeznaczone do wyświetlenia użytkownikowi.
 * Zawiera tylko niezbędne informacje, bez poprawnej odpowiedzi.
 *
 * @param questionId Identyfikator pytania.
 * @param questionText Treść pytania.
 * @param questionType Typ pytania (np. "multiple_choice", "open_ended").
 * @param options Mapa opcji odpowiedzi dla pytań wielokrotnego wyboru (np. {"A": "Opcja A", "B": "Opcja B"}). Null dla pytań otwartych.
 */
public record QuizQuestionDTO(
        Long questionId,
        String questionText,
        String questionType,
        Map<String, String> options
) {}
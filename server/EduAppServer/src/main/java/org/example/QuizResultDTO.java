// plik: src/main/java/org/example/dto/QuizResultDTO.java (opcjonalnie)
package org.example.dto;

import java.time.LocalDateTime;

/**
 * @brief
 * Rekord DTO (Data Transfer Object) reprezentujący skrócony wynik quizu.
 * Może być używany do wyświetlania podstawowych informacji o wyniku, bez szczegółów każdej odpowiedzi.
 *
 * @param completionDate Data i czas ukończenia quizu.
 * @param score Wynik w formacie "poprawne/wszystkie" (np. "7/10").
 * @param percentage Wynik w procentach.
 */
public record QuizResultDTO(
        LocalDateTime completionDate,
        String score,
        double percentage
) {}
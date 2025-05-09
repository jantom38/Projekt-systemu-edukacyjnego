// plik: src/main/java/org/example/dto/QuizResultDTO.java (opcjonalnie)
package org.example.dto;

import java.time.LocalDateTime;

public record QuizResultDTO(
        LocalDateTime completionDate,
        String score,
        double percentage
) {}
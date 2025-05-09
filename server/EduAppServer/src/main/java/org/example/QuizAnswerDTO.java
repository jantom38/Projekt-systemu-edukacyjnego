// plik: src/main/java/org/example/dto/QuizAnswerDTO.java
package org.example.dto;

public record QuizAnswerDTO(
        Long questionId,
        String answer
) {}
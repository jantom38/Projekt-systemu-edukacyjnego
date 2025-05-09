// plik: src/main/java/org/example/dto/QuizQuestionDTO.java
package org.example.dto;

import java.util.Map;

public record QuizQuestionDTO(
        Long questionId,
        String questionText,
        String questionType,
        Map<String, String> options
) {}
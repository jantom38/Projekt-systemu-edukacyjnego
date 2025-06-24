// plik: src/main/java/org/example/dto/QuizAnswerDTO.java
package org.example.dto;

/**
 * @brief
 * Rekord DTO (Data Transfer Object) reprezentujący odpowiedź użytkownika na pojedyncze pytanie quizu.
 * Używany do przenoszenia danych odpowiedzi z klienta do serwera.
 *
 * @param questionId Identyfikator pytania, na które udzielono odpowiedzi.
 * @param answer Tekstowa odpowiedź użytkownika na pytanie.
 */
public record QuizAnswerDTO(
        Long questionId,
        String answer
) {}
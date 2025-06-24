package org.example.database;

/**
 * @brief
 * Enumeracja definiująca dostępne role użytkowników w systemie.
 */
public enum UserRole {
    /**
     * Rola administratora z pełnymi uprawnieniami.
     */
    ADMIN,
    /**
     * Rola nauczyciela.
     */
    TEACHER,
    /**
     * Rola studenta.
     */
    STUDENT
}
package org.example.database;

/**
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
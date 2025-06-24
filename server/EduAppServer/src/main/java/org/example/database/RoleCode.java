package org.example.database;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * @brief
 * Klasa encji reprezentująca kod rejestracyjny dla określonej roli użytkownika.
 * Mapowana jest do tabeli "role_codes" w bazie danych.
 */
@Entity
@Table(name = "role_codes")
public class RoleCode {
    /**
     * Unikalny identyfikator kodu roli.
     * Jest to klucz główny generowany automatycznie.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unikalny ciąg kodu. Nie może być nullem.
     */
    @Column(unique = true, nullable = false)
    private String code;

    /**
     * Rola użytkownika, którą kod nadaje. Przechowywana jako string. Nie może być nullem.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    /**
     * Użytkownik, który utworzył ten kod. Nie może być nullem.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    /**
     * Data i czas utworzenia kodu. Nie może być nullem.
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * Data i czas wygaśnięcia kodu. Nie może być nullem.
     */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Flaga wskazująca, czy kod jest aktywny. Domyślnie true.
     */
    private boolean isActive = true;

    /**
     * Domyślny konstruktor.
     */
    public RoleCode() {}

    /**
     * Konstruktor z parametrami do tworzenia kodu rejestracyjnego.
     * @param code Ciąg kodu.
     * @param role Rola użytkownika.
     * @param creator Użytkownik tworzący kod.
     * @param createdAt Data utworzenia.
     * @param expiresAt Data wygaśnięcia.
     */
    public RoleCode(String code, UserRole role, User creator, LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.code = code;
        this.role = role;
        this.creator = creator;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { this.isActive = active; }
}
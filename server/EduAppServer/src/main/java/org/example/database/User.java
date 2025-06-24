package org.example.database;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Klasa encji reprezentująca użytkownika systemu.
 * Mapowana jest do tabeli "users" w bazie danych.
 */
@Entity
@Table(name = "users")
public class User {
    /**
     * Unikalny identyfikator użytkownika.
     * Jest to klucz główny generowany automatycznie.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nazwa użytkownika (login). Musi być unikalna.
     */
    @Column(unique = true)
    private String username;
    /**
     * Hasło użytkownika.
     */
    private String password;

    /**
     * Rola użytkownika (np. ADMIN, TEACHER, STUDENT). Przechowywana jako string.
     */
    @Enumerated(EnumType.STRING)
    private UserRole role;

    /**
     * Identyfikator kursu, do którego przypisany jest student (opcjonalne, tylko dla studentów).
     */
    private Integer courseId;
    /**
     * Lista wyników quizów powiązanych z tym użytkownikiem.
     * Relacja jeden do wielu z klasą QuizResult.
     * Operacje kaskadowe (np. usunięcie użytkownika usuwa jego wyniki quizów) są włączone.
     * Pole jest ignorowane podczas serializacji JSON.
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private List<QuizResult> quizResults = new ArrayList<>();

    // Gettery i settery
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public Integer getCourseId() { return courseId; }
    public void setCourseId(Integer courseId) { this.courseId = courseId; }
}
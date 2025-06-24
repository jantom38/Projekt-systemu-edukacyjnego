package org.example.database;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

/**
 * @brief
 * Klasa encji reprezentująca powiązanie między użytkownikiem a kursem.
 * Mapowana jest do tabeli "user_courses" w bazie danych.
 */
@Entity
@Table(name = "user_courses")
public class UserCourse {
    /**
     * Unikalny identyfikator powiązania użytkownika z kursem.
     * Jest to klucz główny generowany automatycznie.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Użytkownik przypisany do kursu.
     * Wiele przypisań do kursów może być powiązanych z jednym użytkownikiem.
     * Kolumna 'user_id' jest kluczem obcym i nie może być nullem.
     * Pole jest ignorowane podczas serializacji JSON.
     */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    /**
     * Kurs, do którego użytkownik jest przypisany.
     * Wiele użytkowników może być przypisanych do jednego kursu.
     * Kolumna 'course_id' jest kluczem obcym i nie może być nullem.
     */
    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /**
     * Data i czas, kiedy użytkownik dołączył do kursu. Domyślnie ustawiane na aktualny czas.
     */
    private LocalDateTime joinedAt = LocalDateTime.now();
    /**
     * Flaga wskazująca, czy przypisanie użytkownika do kursu jest aktywne. Domyślnie true.
     */
    private boolean active = true;

    // Konstruktory
    /**
     * Domyślny konstruktor.
     */
    public UserCourse() {}

    /**
     * Konstruktor z parametrami do inicjalizacji powiązania użytkownika z kursem.
     * @param user Użytkownik.
     * @param course Kurs.
     */
    public UserCourse(User user, Course course) {
        this.user = user;
        this.course = course;
    }

    // Gettery i settery
    public Long getId() { return id; }
    public User getUser() { return user; }
    public Course getCourse() { return course; }
    public LocalDateTime getJoinedAt() { return joinedAt; }
    public boolean isActive() { return active; }

    public void setUser(User user) { this.user = user; }
    public void setCourse(Course course) { this.course = course; }
    public void setActive(boolean active) { this.active = active; }
}
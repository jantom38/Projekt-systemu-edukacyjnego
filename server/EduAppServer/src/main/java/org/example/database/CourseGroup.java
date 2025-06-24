package org.example.database;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @brief
 * Klasa encji reprezentująca grupę kursów.
 * Mapowana jest do tabeli "course_groups" w bazie danych.
 */
@Entity
@Table(name = "course_groups")
public class CourseGroup {
    /**
     * Unikalny identyfikator grupy kursów.
     * Jest to klucz główny generowany automatycznie.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nazwa grupy kursów. Nie może być nullem.
     */
    @Column(nullable = false)
    private String name;

    /**
     * Opis grupy kursów.
     */
    private String description;

    /**
     * Nauczyciel odpowiedzialny za grupę kursów.
     * Wiele grup kursów może być przypisanych do tego samego nauczyciela.
     * Kolumna 'teacher_id' jest kluczem obcym i nie może być nullem.
     * Pole jest ignorowane podczas serializacji JSON.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    @JsonIgnore
    private User teacher;

    /**
     * Lista kursów należących do tej grupy.
     * Relacja jeden do wielu z klasą Course.
     * Operacje kaskadowe (np. usunięcie grupy usuwa powiązane kursy) są włączone.
     */
    @OneToMany(mappedBy = "courseGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Course> courses = new ArrayList<>();

    // Gettery i Settery
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public User getTeacher() { return teacher; }
    public void setTeacher(User teacher) { this.teacher = teacher; }
    public List<Course> getCourses() { return courses; }
    public void setCourses(List<Course> courses) { this.courses = courses; }
}
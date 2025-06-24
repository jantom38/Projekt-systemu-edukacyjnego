package org.example.database;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/**
 * Klasa encji reprezentująca kurs.
 * Mapowana jest do tabeli "Courses" w bazie danych.
 */
@Entity
@Table(name = "Courses")
public class Course {
    /**
     * Unikalny identyfikator kursu.
     * Jest to klucz główny generowany automatycznie.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nazwa kursu. Musi być unikalna.
     */
    @Column(unique = true)
    private String courseName;
    /**
     * Opis kursu.
     */
    private String description;
    /**
     * Klucz dostępu do kursu.
     */
    private String accessKey;
    /**
     * Nauczyciel przypisany do kursu.
     * Wiele kursów może mieć tego samego nauczyciela.
     * Kolumna 'teacher_id' jest kluczem obcym i nie może być nullem.
     * Pole jest ignorowane podczas serializacji JSON.
     */
    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    @JsonIgnore
    private User teacher;


    /**
     * Grupa kursów, do której należy ten kurs.
     * Wiele kursów może należeć do jednej grupy kursów.
     * Kolumna 'course_group_id' jest kluczem obcym.
     * Pole jest ignorowane podczas serializacji JSON.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_group_id")
    @JsonIgnore
    private CourseGroup courseGroup;

    // Gettery i settery
    public Long getId() { return id; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
    public User getTeacher() {return teacher;}
    public void setTeacher(User teacher) {this.teacher = teacher;}

    public CourseGroup getCourseGroup() { return courseGroup; }
    public void setCourseGroup(CourseGroup courseGroup) { this.courseGroup = courseGroup; }
}
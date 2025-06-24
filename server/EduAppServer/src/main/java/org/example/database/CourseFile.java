package org.example.database;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

/**
 * @brief
 * Klasa encji reprezentująca plik przypisany do kursu.
 */
@Entity
public class CourseFile {
    /**
     * Unikalny identyfikator pliku.
     * Jest to klucz główny generowany automatycznie.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nazwa pliku.
     */
    private String fileName;
    /**
     * URL do pliku.
     */
    private String fileUrl;

    /**
     * Kurs, do którego należy plik.
     * Wiele plików może być przypisanych do jednego kursu.
     * Kolumna 'course_id' jest kluczem obcym.
     * Pole jest ignorowane podczas serializacji JSON.
     */
    @ManyToOne
    @JoinColumn(name = "course_id")
    @JsonIgnore
    private Course course;

    // Konstruktory
    /**
     * Domyślny konstruktor.
     */
    public CourseFile() {}

    /**
     * Konstruktor z parametrami do inicjalizacji pliku kursu.
     * @param fileName Nazwa pliku.
     * @param fileUrl URL do pliku.
     * @param course Kurs, do którego plik jest przypisany.
     */
    public CourseFile(String fileName, String fileUrl, Course course) {
        this.fileName = fileName;
        this.fileUrl = fileUrl;
        this.course = course;
    }

    // Gettery i settery
    public Long getId() { return id; }
    public String getFileName() { return fileName; }
    public String getFileUrl() { return fileUrl; }
    public Course getCourse() { return course; }
    // ... settery ...
    public void setId(Long id) { this.id = id; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public void setCourse(Course course) { this.course = course; }
}
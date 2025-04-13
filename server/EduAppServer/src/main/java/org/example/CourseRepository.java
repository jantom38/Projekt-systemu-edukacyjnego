package org.example;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByCourseName(String courseName);
    boolean existsByAccessKey(String accessKey); // Zmiana z accesskey na accessKey
    Optional<Course> findByAccessKey(String accessKey); // Zmiana z accesskey na accessKey
    void deleteByCourseName(String courseName);
}
package org.example.DataBaseRepositories;

import org.example.database.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {
    Optional<Course> findByCourseName(String courseName);
    boolean existsByAccessKey(String accessKey); // Zmiana z accesskey na accessKey
    Optional<Course> findByAccessKey(String accessKey); // Zmiana z accesskey na accessKey
    void deleteByCourseName(String courseName);
    List<Course> findByTeacherUsername(String username);
    Optional<Course> findByIdAndTeacherUsername(Long id, String username);
}
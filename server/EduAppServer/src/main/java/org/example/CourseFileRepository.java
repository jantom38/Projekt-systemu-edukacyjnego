package org.example;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CourseFileRepository extends JpaRepository<CourseFile, Long> {
    List<CourseFile> findByCourseId(Long courseId);
}
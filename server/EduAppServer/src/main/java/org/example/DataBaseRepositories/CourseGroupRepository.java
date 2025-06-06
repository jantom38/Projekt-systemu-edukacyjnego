package org.example.DataBaseRepositories;

import org.example.database.CourseGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseGroupRepository extends JpaRepository<CourseGroup, Long> {
    List<CourseGroup> findByTeacherUsername(String username);
}
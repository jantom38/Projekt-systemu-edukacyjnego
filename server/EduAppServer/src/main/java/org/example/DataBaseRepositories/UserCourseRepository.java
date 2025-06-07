package org.example.DataBaseRepositories;

import org.example.database.UserCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserCourseRepository extends JpaRepository<UserCourse, Long> {
    List<UserCourse> findByUserId(Long userId);
    boolean existsByUserIdAndCourseId(Long userId, Long courseId);
    List<UserCourse> findByCourseId(Long courseId);
    Optional<UserCourse> findByUserIdAndCourseId(Long userId, Long courseId);
    void deleteByCourseId(long courseId);
}

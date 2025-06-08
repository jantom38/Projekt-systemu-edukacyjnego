// QuizResultRepository.java
package org.example.DataBaseRepositories;

import jakarta.transaction.Transactional;
import org.example.database.QuizResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {
    List<QuizResult> findByUserId(Long userId);
    List<QuizResult> findByQuizId(Long quizId);
    List<QuizResult> findByUserIdAndQuizId(Long userId, Long quizId);

}
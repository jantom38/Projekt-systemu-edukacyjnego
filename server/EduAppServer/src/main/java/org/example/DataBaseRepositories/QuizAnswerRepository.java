package org.example.DataBaseRepositories;

import org.example.database.QuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {
    List<QuizAnswer> findByQuizResultId(Long quizResultId);

}
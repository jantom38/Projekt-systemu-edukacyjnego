package org.example.DataBaseRepositories;

import org.example.database.QuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {
    @Query("SELECT qa FROM QuizAnswer qa JOIN FETCH qa.question WHERE qa.quizResult.id = :quizResultId")
    List<QuizAnswer> findByQuizResultIdWithQuestions(@Param("quizResultId") Long quizResultId);
}
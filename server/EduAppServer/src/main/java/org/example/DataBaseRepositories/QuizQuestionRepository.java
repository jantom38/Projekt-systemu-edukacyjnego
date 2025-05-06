package org.example.DataBaseRepositories;

import org.example.database.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
    List<QuizQuestion> findByQuizId(Long quizId);
    @Modifying
    @Query("DELETE FROM QuizQuestion q WHERE q.quiz.id = :quizId")
    void deleteByQuizId(@Param("quizId") Long quizId);
}
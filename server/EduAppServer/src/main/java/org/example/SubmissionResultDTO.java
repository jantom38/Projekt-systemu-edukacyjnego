package org.example;

import lombok.Data;

@Data
public class SubmissionResultDTO {
    private boolean success;
    private String score;
    private int correctAnswers;
    private int totalQuestions;
    private double percentage;

    public SubmissionResultDTO(boolean success, String score, int correctAnswers, int totalQuestions, double percentage) {
        this.success = success;
        this.score = score;
        this.correctAnswers = correctAnswers;
        this.totalQuestions = totalQuestions;
        this.percentage = percentage;
    }
}
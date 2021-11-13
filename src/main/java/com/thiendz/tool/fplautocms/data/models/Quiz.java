package com.thiendz.tool.fplautocms.data.models;

import com.thiendz.tool.fplautocms.utils.NumberUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Quiz implements Comparable<Quiz> {

    private String url;
    private String name;
    private double score;
    private double scorePossible;
    private List<QuizQuestion> quizQuestions;

    public Quiz(String url) {
        this.url = url;
    }

    @Override
    public int compareTo(Quiz quiz) {
        if (quiz == null) {
            return 1;
        }
        int num = NumberUtils.getInt(this.getName());
        int num2 = NumberUtils.getInt(quiz.getName());
        if (num == -1) {
            return -1;
        }
        if (num2 == -1) {
            return 1;
        }
        return num - num2;
    }

}

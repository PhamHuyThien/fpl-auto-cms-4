package com.thiendz.tool.fplautocms.models;

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

    @Override
    public int compareTo(Quiz quiz) {
        if (quiz == null) {
            return 1;
        }
        Integer thisQUizNumber = NumberUtils.getInt(this.getName());
        Integer thatQuizNumber = NumberUtils.getInt(quiz.getName());
        if (thisQUizNumber == null)
            return -1;
        if (thatQuizNumber == null)
            return 1;
        return thisQUizNumber - thatQuizNumber;
    }

}

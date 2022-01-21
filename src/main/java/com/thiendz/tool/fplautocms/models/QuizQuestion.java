package com.thiendz.tool.fplautocms.models;


import com.thiendz.tool.fplautocms.utils.NumberUtils;
import com.thiendz.tool.fplautocms.utils.enums.QuizQuestionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class QuizQuestion implements Comparable<QuizQuestion> {
    private QuizQuestionType type;
    private String name;
    private String question;
    private String key;
    private List<String> value;
    private List<String> listValue;
    private int input;
    private boolean multiChoice;
    private int test;
    private boolean correct;

    @Override
    public int compareTo(QuizQuestion thatQuizQuestion) {
        Integer thisQuizNumber = NumberUtils.getInt(this.getName());
        Integer thatQuizNumber = NumberUtils.getInt(thatQuizQuestion.getName());
        if (thisQuizNumber == null)
            return -1;
        if (thatQuizNumber == null)
            return 1;
        return thisQuizNumber - thatQuizNumber;
    }
}

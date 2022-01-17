package com.thiendz.tool.fplautocms.models;


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
public class QuizQuestion {
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
}

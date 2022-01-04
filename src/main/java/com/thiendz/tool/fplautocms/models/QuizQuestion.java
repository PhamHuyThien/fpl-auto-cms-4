package com.thiendz.tool.fplautocms.models;


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
    private String name;
    private String type; // [text, checkbox, radio]
    private String question;
    private String key; //key request
    private List<String> listValue;// value request
    //số lượng câu hỏi trên một question (dành cho type text)
    private int amountInput;
    //có chọn nhiều đáp ans ko
    private boolean multiChoice;
    //giá trị request đi
    private String selectValue;
    //số lần thử
    private int testCount;
    //hoàn thành
    private boolean correct;
}

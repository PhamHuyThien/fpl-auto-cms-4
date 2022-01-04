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
public class Course {
    private String name;
    private String id;
    private String number;
    private String index;
    private String refundUrl;
    private List<Quiz> quizList;
}

package com.thiendz.tool.fplautocms.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jsoup.nodes.Element;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class QuizDto {
    String chapterId;
    String sequentialId;
    Element element;
}

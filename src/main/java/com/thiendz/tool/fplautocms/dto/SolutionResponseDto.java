package com.thiendz.tool.fplautocms.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class SolutionResponseDto {
    Boolean progress_changed;
    Double total_possible;
    String success;
    Double current_score;
    Long attempts_used;
    String contents;
    String html;
}

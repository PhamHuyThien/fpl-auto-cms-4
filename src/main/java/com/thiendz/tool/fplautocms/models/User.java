package com.thiendz.tool.fplautocms.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
    private String cookie;
    private String csrf_token;
    private String username;
    private String user_id;
    private String email;
    private List<Course> courses;
}

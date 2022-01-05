package com.thiendz.tool.fplautocms.dto.callback;

import com.thiendz.tool.fplautocms.models.Quiz;

@FunctionalInterface
public interface CallbackSolution {
    void call(double scorePresent, int status, Quiz quiz);
}

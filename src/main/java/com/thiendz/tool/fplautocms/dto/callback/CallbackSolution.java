package com.thiendz.tool.fplautocms.dto.callback;

@FunctionalInterface
public interface CallbackSolution {
    void call(double scorePresent, int status);
}

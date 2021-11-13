package com.thiendz.tool.fplautocms.utils.excepts;

public class InputException extends Exception {
    private final String message;

    public InputException(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public String toString() {
        return message;
    }
}

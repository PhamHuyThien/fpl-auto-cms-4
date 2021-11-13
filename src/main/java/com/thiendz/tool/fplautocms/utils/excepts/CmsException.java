package com.thiendz.tool.fplautocms.utils.excepts;

public class CmsException extends Exception {
    private final String message;

    public CmsException(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public String toString() {
        return message;
    }
}

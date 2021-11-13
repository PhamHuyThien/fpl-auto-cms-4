package com.thiendz.tool.fplautocms.utils;

import java.util.List;

public class NumberUtils {
    public static int getInt(String text) {
        List<String> numbers = StringUtils.regex("([0-9]+)", text, String.class);
        if (!numbers.isEmpty()) {
            return Integer.parseInt(numbers.get(0));
        }
        return -1;
    }
}

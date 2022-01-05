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

    public static double roundReal(double d, int index) {
        int j = 1;
        for (int i = 0; i < index; i++) {
            j *= 10;
        }
        return (double) Math.floor(d * j) / j;
    }

}

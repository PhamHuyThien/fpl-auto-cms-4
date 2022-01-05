package com.thiendz.tool.fplautocms.utils;

import java.util.Date;

public class DateUtils {
    public static long getCurrentMilis() {
        return new Date().getTime();
    }
    public static String toStringDate(int second) {
        String result = "";
        int numberOfMinutes;
        int numberOfSeconds;
        numberOfMinutes = ((second % 86400) % 3600) / 60;
        numberOfSeconds = ((second % 86400) % 3600) % 60;
        if (numberOfMinutes > 0) {
            result += numberOfMinutes + " phút ";
        }
        if (numberOfSeconds > 0) {
            result += numberOfSeconds + " giây";
        }
        return result;
    }
}

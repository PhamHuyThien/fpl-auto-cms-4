package com.thiendz.tool.fplautocms.utils;

import java.util.Date;

public class DateUtils {
    public static long getCurrentMilis() {
        return new Date().getTime();
    }

    public static String toStringDate(int second) {
        String result = "";
        int numberOfMinutes, numberOfSeconds, numberOfHours;
        numberOfHours = second / 3600;
        numberOfMinutes = ((second % 86400) % 3600) / 60;
        numberOfSeconds = ((second % 86400) % 3600) % 60;
        if (numberOfHours > 0)
            result += numberOfHours + " giờ ";
        if (numberOfMinutes > 0)
            result += numberOfMinutes + " phút ";
        if (numberOfSeconds > 0)
            result += numberOfSeconds + " giây";
        return result;
    }
}

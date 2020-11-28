package com.vv.personal.twm.twm.util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static com.vv.personal.twm.twm.constants.Constants.EMPTY_STR;

/**
 * @author Vivek
 * @since 08/11/20
 */
public class TimeUtil {

    public static LocalTime transmuteToLocalTime(String inputTime) {
        String pattern = EMPTY_STR;
        if (inputTime.matches("[0-9]{2}[0-9]{2}[0-9]{2}")) pattern = "HHmmss";
        else if (inputTime.matches("[0-9]{2}:[0-9]{2}:[0-9]{2}")) pattern = "HH:mm:ss";
        else if (inputTime.matches("[0-9]{2}[0-9]{2}")) pattern = "HHmm";
        else if (inputTime.matches("[0-9]{2}:[0-9]{2}")) pattern = "HH:mm";

        if (pattern.isEmpty()) {
            System.out.printf("Couldn't decipher '%s', cannot convert!%n", inputTime);
            return null;
        }
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(pattern);
        try {
            return LocalTime.parse(inputTime, dtf);
        } catch (Exception e) {
            System.out.printf("Unable to extract time out of '%s'. Exception: %s%n", inputTime, e);
        }
        return null;
    }
}

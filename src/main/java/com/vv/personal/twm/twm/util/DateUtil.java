package com.vv.personal.twm.twm.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static com.vv.personal.twm.twm.constants.Constants.EMPTY_STR;

/**
 * @author Vivek
 * @since 08/11/20
 */
public class DateUtil {

    public static LocalDate transmuteToLocalDate(String inputDate) {
        String pattern = EMPTY_STR;
        if (inputDate.matches("[0-9]{4}[0-9]{2}[0-9]{2}")) pattern = "yyyyMMdd";
        else if (inputDate.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) pattern = "yyyy-MM-dd";
        else if (inputDate.matches("[0-9]{2}-[0-9]{2}-[0-9]{4}")) pattern = "dd-MM-yyyy";
        else if (inputDate.matches("[0-9]{2}/[0-9]{2}/[0-9]{4}")) pattern = "MM/dd/yyyy";
        else if (inputDate.matches("[0-9]{2}/[0-9]{2}/[0-9]{2}")) pattern = "MM/dd/yy";

        if (pattern.isEmpty()) {
            System.out.printf("Couldn't decipher '%s', cannot convert!%n", inputDate);
            return null;
        }
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern(pattern);
        try {
            return LocalDate.parse(inputDate, dtf);
        } catch (Exception e) {
            System.out.printf("Unable to extract date out of '%s'. Exception: %s%n", inputDate, e);
        }
        return null;
    }

}

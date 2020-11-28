package com.vv.personal.twm.twm.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Calendar;

/**
 * @author Vivek
 * @since 28/11/20
 */
public class GenericUtil {

    public static String generateCurrentDateAsString() {
        Calendar calendar = Calendar.getInstance();
        return String.format("%d%d%d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
    }

    public static String generateCurrentDateTimeAsString() {
        Calendar calendar = Calendar.getInstance();
        return String.format("%d%d%d-%d:%d", calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
    }

    public static long generateInsertionTime(String insertionTime) {
        String[] insertionTimeSplit = insertionTime.split("-");
        LocalDate date = DateUtil.transmuteToLocalDate(insertionTimeSplit[0]);
        LocalTime time = TimeUtil.transmuteToLocalTime(insertionTimeSplit[1]);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, date.getDayOfMonth());
        calendar.set(Calendar.MONTH, date.getMonthValue() - 1);
        calendar.set(Calendar.YEAR, date.getYear());
        calendar.set(Calendar.HOUR_OF_DAY, time.getHour());
        calendar.set(Calendar.MINUTE, time.getMinute());
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }
}

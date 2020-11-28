package com.vv.personal.twm.twm.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.LocalDate;

import static com.vv.personal.twm.twm.util.DateUtil.transmuteToLocalDate;
import static org.junit.Assert.*;

/**
 * @author Vivek
 * @since 28/11/20
 */
@RunWith(JUnit4.class)
public class DateUtilTest {

    @Test
    public void testTransmuteToLocalDate() {
        LocalDate date = transmuteToLocalDate("20201108");
        assertNotNull(date);
        assertEquals(8, date.getDayOfMonth());
        assertEquals(11, date.getMonthValue());
        assertEquals(2020, date.getYear());

        date = transmuteToLocalDate("2020-11-08");
        assertNotNull(date);
        assertEquals(8, date.getDayOfMonth());
        assertEquals(11, date.getMonthValue());
        assertEquals(2020, date.getYear());

        date = transmuteToLocalDate("08-11-2020");
        assertNotNull(date);
        assertEquals(8, date.getDayOfMonth());
        assertEquals(11, date.getMonthValue());
        assertEquals(2020, date.getYear());

        date = transmuteToLocalDate("11/08/2020");
        assertNotNull(date);
        assertEquals(8, date.getDayOfMonth());
        assertEquals(11, date.getMonthValue());
        assertEquals(2020, date.getYear());

        date = transmuteToLocalDate("11/08/20");
        assertNotNull(date);
        assertEquals(8, date.getDayOfMonth());
        assertEquals(11, date.getMonthValue());
        assertEquals(2020, date.getYear());

        date = transmuteToLocalDate("");
        assertNull(date);

        date = transmuteToLocalDate("11/8/2020");
        assertNull(date);
    }
}
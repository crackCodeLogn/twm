package com.vv.personal.twm.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.LocalTime;

import static com.vv.personal.twm.util.TimeUtil.transmuteToLocalTime;
import static org.junit.Assert.*;

/**
 * @author Vivek
 * @since 08/11/20
 */
@RunWith(JUnit4.class)
public class TimeUtilTest {

    @Test
    public void testTransmuteToLocalTime() {
        LocalTime time = transmuteToLocalTime("191748");
        assertNotNull(time);
        assertEquals(19, time.getHour());
        assertEquals(17, time.getMinute());
        assertEquals(48, time.getSecond());

        time = transmuteToLocalTime("19:18:48");
        assertNotNull(time);
        assertEquals(19, time.getHour());
        assertEquals(18, time.getMinute());
        assertEquals(48, time.getSecond());

        time = transmuteToLocalTime("");
        assertNull(time);

        time = transmuteToLocalTime("19/17/48");
        assertNull(time);
    }

}
package com.vv.personal.twm.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static com.vv.personal.twm.util.GenericUtil.generateInsertionTime;
import static org.junit.Assert.assertEquals;

/**
 * @author Vivek
 * @since 28/11/20
 */
@RunWith(JUnit4.class)
public class GenericUtilTest {

    @Test
    public void testGenerateInsertionTime() {
        assertEquals(1606607040000L, generateInsertionTime("20201128-18:44"));
    }

}
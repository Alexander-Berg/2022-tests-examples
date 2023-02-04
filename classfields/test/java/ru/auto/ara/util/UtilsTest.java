package ru.auto.ara.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ru.auto.core_ui.util.Consts;

/**
 * Created by aleien on 13.01.17.
 */

public class UtilsTest {

    @Test
    public void checkConst_weekInMillis_604800000() {
        assertEquals(604800000, Consts.WEEK_MS);
    }
}

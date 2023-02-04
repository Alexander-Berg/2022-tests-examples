package ru.yandex.qe.testing.utils;

import java.io.ByteArrayOutputStream;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestLogUtilsTest {
    @Test
    public void testLogged() {
        ByteArrayOutputStream logOutStream = TestLogUtils.createLogInterceptor();
        LoggerFactory.getLogger("someLogger").info("ping-pong4");
        assertTrue(TestLogUtils.logged(logOutStream, "pong\\d"));
        assertTrue(TestLogUtils.notLogged(logOutStream, "pong-ping"));
    }
    @Test
    public void testLoggedNTimes() {
        ByteArrayOutputStream logOutStream = TestLogUtils.createLogInterceptor("newLogger");
        LoggerFactory.getLogger("newLogger").info("bla-bla-bla!");
        assertEquals(TestLogUtils.timesLogged(logOutStream, "bla[-!]"), 3);
        assertEquals(TestLogUtils.timesLogged(logOutStream, "bla-"), 2);
        assertEquals(TestLogUtils.timesLogged(logOutStream, "pp"), 0);
    }

}
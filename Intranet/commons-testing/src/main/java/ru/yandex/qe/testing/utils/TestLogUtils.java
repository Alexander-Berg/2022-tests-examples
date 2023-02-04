package ru.yandex.qe.testing.utils;

import java.io.ByteArrayOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.OutputStreamAppender;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author alisovenko 01.08.14
 */
public class TestLogUtils {
    private TestLogUtils() {
    }

    public static final int BUFFER_SIZE = 4096;

    public static ByteArrayOutputStream createLogInterceptor() {
        return createLogInterceptor(null);
    }

    public static ByteArrayOutputStream createLogInterceptor(String logger) {
        ByteArrayOutputStream logOutStream = new ByteArrayOutputStream(BUFFER_SIZE);
        logOutStream.reset();

        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("%-4relative [%thread] %-5level %logger{35} - %msg%n");
        encoder.start();

        OutputStreamAppender interceptingAppender = new OutputStreamAppender();
        interceptingAppender.setEncoder(encoder);
        interceptingAppender.setOutputStream(logOutStream);

        interceptingAppender.setContext(loggerContext);

        interceptingAppender.start();

        Logger logbackLogger;
        if (logger == null) {
            logbackLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        } else {
            logbackLogger = loggerContext.getLogger(logger);
        }
        logbackLogger.addAppender(interceptingAppender);

        return logOutStream;
    }

    public static void assertNotLogged(ByteArrayOutputStream logOutStream, String pattern) {
        assertTrue(notLogged(logOutStream, pattern),
                String.format("'%s' pattern found in logs, but was not expected to", pattern));
    }

    public static void assertLogged(ByteArrayOutputStream logOutStream, String pattern) {
        assertTrue(logged(logOutStream, pattern), String.format("'%s' pattern not found in logs", pattern));
    }

    public static void assertLogged(ByteArrayOutputStream logOutStream, String phrase, int times) {
        int cnt = timesLogged(logOutStream, phrase);

        assertEquals(cnt, times,
                String.format("'%s' pattern found in log:\n expected: %d, but in fact %d times", phrase, times, cnt));
    }

    public static boolean notLogged(ByteArrayOutputStream logOutStream, String pattern) {
        return !logged(logOutStream, pattern);
    }

    public static boolean logged(ByteArrayOutputStream logOutStream, String pattern) {
        String fullLog = logOutStream.toString();

        Pattern p = Pattern.compile(pattern);
        Matcher matcher = p.matcher(fullLog);

        return matcher.find();
    }

    public static int timesLogged(ByteArrayOutputStream logOutStream, String pattern) {
        String fullLog = logOutStream.toString();

        Pattern p = Pattern.compile(pattern);
        Matcher matcher = p.matcher(fullLog);

        int cnt = 0;
        while (matcher.find()) {
            cnt++;
        }

        return cnt;
    }

}

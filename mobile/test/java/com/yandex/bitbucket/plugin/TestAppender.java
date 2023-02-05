package com.yandex.bitbucket.plugin;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.util.ArrayList;
import java.util.List;

public class TestAppender extends AppenderBase<LoggingEvent> {
    public static List<LoggingEvent> events = new ArrayList<>();

    @Override
    protected void append(LoggingEvent e) {
        events.add(e);
    }

    public static void clearEvents() {
        events.clear();
    }

    public static LoggingEvent getLastElement() {
        if (events.size() != 0) {
            return events.get(events.size() - 1);
        } else {
            return null;
        }
    }
}

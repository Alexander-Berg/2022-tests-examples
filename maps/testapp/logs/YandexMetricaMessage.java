package com.yandex.maps.testapp.logs;

import com.yandex.runtime.recording.LoggingLevel;

public class YandexMetricaMessage {

    private LoggingLevel level;
    private String msg;
    private long time;

    public YandexMetricaMessage(LoggingLevel level, String msg) {
        this.level = level;
        this.msg = msg;
        this.time = System.currentTimeMillis();
    }

    public LoggingLevel getLevel() {
        return level;
    }

    public String getMessage() {
        return msg;
    }

    public long getTime() {
        return time;
    }
}

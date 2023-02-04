package com.yandex.maps.testapp.logs;

import android.graphics.Color;

import com.yandex.runtime.logging.LogMessage;

public class LogEvent {

    private int color;
    private long time;
    private String type;
    private String message;
    private String scope;
    private String verbose = "";


    public LogEvent(LogMessage msg) {
        scope = msg.getScope();
        verbose = msg.getVerboseInfo();
        time = msg.getTime();
        message = msg.getMessage();
        switch (msg.getLevel()) {
            case ERROR:
                color = Color.RED;
                type = "log-e";
                break;
            case WARNING:
                color = Color.YELLOW;
                type = "log-w";
                break;
            case INFO:
                color = Color.WHITE;
                type = "log-i";
                break;
            case DEBUG:
                color = Color.GREEN;
                type = "log-d";
                break;
        }
    }

    public LogEvent(YandexMetricaMessage msg) {
        time = msg.getTime();
        type = "Metrica";
        message = scope = msg.getMessage();
        switch (msg.getLevel()) {
            case NORMAL:
                color = Color.CYAN;
                type = "metrica-n";
                break;
            case VERBOSE:
                color = Color.MAGENTA;
                type = "metrica-v";
                break;
        }
    }

    public long getTime() {
        return time;
    }

    public String getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public int getColor() {
        return color;
    }

    public String getVerboseInfo() {
        return verbose;
    }

    public String getScope() {
        return scope;
    }
}

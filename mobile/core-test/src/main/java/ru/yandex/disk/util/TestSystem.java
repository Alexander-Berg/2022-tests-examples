package ru.yandex.disk.util;

import java.util.HashMap;
import java.util.Map;

public class TestSystem extends System {

    private final Map<String, String> env = new HashMap<>();
    private long currentTime;

    public TestSystem() {
        currentTime = java.lang.System.currentTimeMillis();
    }

    public void setenv(String name, String value) {
        env.put(name, value);
    }

    @Override
    public String getenv(String name) {
        return env.get(name);
    }

    @Override
    public long currentTimeMillis() {
        return currentTime;
    }

    public void setCurrentTime(long currentTime) {
        this.currentTime = currentTime;
    }
}
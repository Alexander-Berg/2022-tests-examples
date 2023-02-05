package com.yandex.frankenstein;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static org.mockito.Mockito.mock;

@SuppressWarnings("unused")
public class MockLogManager extends LogManager {

    private final Map<String, Logger> mLoggers = new ConcurrentHashMap<>();

    @Override
    public Logger getLogger(final String name) {
        return mLoggers.computeIfAbsent(name, key -> mock(Logger.class));
    }
}

package ru.yandex.disk.test;

import ru.yandex.disk.util.TraceInfo;

public abstract class OpenInfo {
    private final TraceInfo openHere;
    private final String resourceType;

    public OpenInfo(String resourceType) {
        this.resourceType = resourceType;
        openHere = new TraceInfo(resourceType + " was opened here");
    }

    protected abstract boolean isClosed();

    public void throwException() {
        String message = resourceType + " is not closed, see caused exception trace";
        throw new IllegalStateException(message, openHere);
    }
}

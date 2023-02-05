package ru.yandex.disk.test;

import java.util.LinkedList;

public class ResourceTracker {
    private final LinkedList<OpenInfo> resources = new LinkedList<>();

    protected void add(OpenInfo e) {
        resources.add(e);
    }

    public void checkState() {
        for (OpenInfo info : resources) {
            if (!info.isClosed()) {
                info.throwException();
            }
        }
    }
}

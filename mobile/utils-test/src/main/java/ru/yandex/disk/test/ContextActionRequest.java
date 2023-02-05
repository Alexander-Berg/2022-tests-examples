package ru.yandex.disk.test;

import android.content.Intent;
import ru.yandex.disk.util.Exceptions;

public class ContextActionRequest {
    private final Intent intent;
    private final Exception trace;

    public ContextActionRequest(Intent intent) {
        this.intent = intent;
        this.trace = new Exception();
    }

    public Exception getTrace() {
        return trace;
    }

    public Intent getIntent() {
        return intent;
    }

    @Override
    public String toString() {
        return "{" + intent + " from:\n" + Exceptions.toString(trace) + "\n}";
    }
}

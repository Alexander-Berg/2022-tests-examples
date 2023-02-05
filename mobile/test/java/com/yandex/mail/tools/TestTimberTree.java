package com.yandex.mail.tools;

import android.util.Log;

import java.util.concurrent.ConcurrentHashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import timber.log.Timber;

import static com.yandex.mail.util.Utils.requireNotNull;

public class TestTimberTree extends Timber.DebugTree {

    @NonNull
    private final ConcurrentHashMap<Integer, StringBuffer> logMap = new ConcurrentHashMap<>();

    @Override
    protected void log(int priority, @NonNull String tag, @NonNull String message, @Nullable Throwable t) {
        super.log(priority, tag, message, t);

        StringBuffer log = logMap.get(priority);
        if (log == null) {
            logMap.putIfAbsent(priority, new StringBuffer());
        }
        log = requireNotNull(logMap.get(priority));
        //noinspection StringConcatenationInsideStringBufferAppend  Should not intersect with other threads.
        log.append(tag + ":" + message + (t == null ? "" : t.toString()));
    }

    public void clear() {
        logMap.clear();
    }

    @NonNull
    public String getLog(int priority) {
        final StringBuffer log = logMap.get(priority);
        return log != null ? log.toString() : "";
    }

    @NonNull
    public String getLogD() {
        return getLog(Log.DEBUG);
    }
}

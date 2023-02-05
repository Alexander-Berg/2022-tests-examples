package ru.yandex.yandexnavi.logger;

import java.util.Arrays;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Handy implementation for tests
 * Has no dependency on the Android framework
 */
public class TestLogWriter implements LogWriter {
    @Override
    public void log(@NonNull Logger.Severity severity, @NonNull String tag,
                    @Nullable String message, @Nullable Throwable throwable) {
        println(severity, tag, message, throwable);
    }

    @Override
    public void e(@NonNull String tag, @Nullable String message, @Nullable Throwable throwable) {
        println(Logger.Severity.ERROR, tag, message, throwable);
    }

    private void println(@Nullable Logger.Severity severity, @NonNull String tag,
                         @Nullable String message, @Nullable Throwable throwable) {
        final String throwableString;
        if (throwable != null) {
            throwableString = ", " + throwable.getClass().getSimpleName() + ", "
                    + throwable.getMessage() + ", " + Arrays.toString(throwable.getStackTrace());
        } else {
            throwableString = "";
        }
        System.out.println((severity != null ? severity.name() : "")
                + "\\" + tag + ":" + message + throwableString);
    }
}


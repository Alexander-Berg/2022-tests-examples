package com.yandex.mobile.realty.testing;


import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.utils.ILogger;

public class NullLogger implements ILogger {

    private static final ILogger sThis = new com.android.utils.NullLogger();

    public static ILogger getLogger() {
        return sThis;
    }

    @Override
    public void error(@Nullable Throwable t, @Nullable String errorFormat, Object... args) {
        // ignore
    }

    @Override
    public void warning(@NonNull String warningFormat, Object... args) {
        // ignore
    }

    @Override
    public void info(@NonNull String msgFormat, Object... args) {
        // ignore
    }

    @Override
    public void verbose(@NonNull String msgFormat, Object... args) {
        // ignore
    }

}

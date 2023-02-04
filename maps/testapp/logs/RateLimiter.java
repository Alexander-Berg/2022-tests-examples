package com.yandex.maps.testapp.logs;

import android.os.Handler;
import java.lang.Runnable;
import java.lang.System;

// Rate limiter which works in UI thread
class RateLimiter {
    private long period;
    private long previousTime = 0;
    private boolean hasDeferredEvent = false;
    private final Handler handler = new Handler();

    RateLimiter(long period) {
        this.period = period;
    }

    public void run(final Runnable runnable) {
        long time = System.currentTimeMillis();
        if (time - previousTime > period) {
            previousTime = time;
            runnable.run();
        } else {
            if (hasDeferredEvent)
                return;

            hasDeferredEvent = true;
            handler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        hasDeferredEvent = false;
                        previousTime = System.currentTimeMillis();
                        runnable.run();
                    }
                },
                period - (time - previousTime));
        }
    }
}

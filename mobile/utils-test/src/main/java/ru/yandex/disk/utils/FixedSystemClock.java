package ru.yandex.disk.utils;

import ru.yandex.disk.util.SystemClock;

public class FixedSystemClock extends SystemClock {

    private long time;

    public FixedSystemClock() {
        this(0);
    }

    public FixedSystemClock(final long now) {
        time = now;
    }

    public void setElapsedRealtime(final long time) {
        this.time = time;
    }

    @Override
    public long elapsedRealtime() {
        return time;
    }

    @Override
    public void sleep(final long ms) {
        time += ms;
    }

    @Override
    public long currentTimeMillis() {
        return time;
    }

    public void move(final long ms) {
        time += ms;
    }

}

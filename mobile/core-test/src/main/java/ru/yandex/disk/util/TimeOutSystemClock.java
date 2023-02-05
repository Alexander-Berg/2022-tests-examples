package ru.yandex.disk.util;

public class TimeOutSystemClock extends SystemClock {

    private long time;
    private final long timeout;

    public TimeOutSystemClock(final long timeout) {
        this.timeout = timeout;
    }

    @Override
    public long elapsedRealtime() {
        return time;
    }

    @Override
    public void sleep(final long ms) {
        time += ms;
        if (time >= timeout) {
            atLastMoment();
        }
    }

    @Override
    public long currentTimeMillis() {
        return time;
    }

    protected void atLastMoment() {
    }

}

package ru.yandex.disk.util;

public class SystemClockWrapper extends SystemClock {

    private SystemClock wrappee;

    public void setWrappee(final SystemClock wrappee) {
        this.wrappee = wrappee;
    }

    public long elapsedRealtime() {
        return wrappee.elapsedRealtime();
    }

    public void sleep(final long ms) {
        wrappee.sleep(ms);
    }

    @Override
    public long currentTimeMillis() {
        return wrappee.currentTimeMillis();
    }

    public SystemClock getWrappee() {
        return wrappee;
    }

}

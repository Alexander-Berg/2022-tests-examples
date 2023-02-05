package com.yandex.mail.test;

import com.yandex.mail.proxy.TimePreferences;

public class TestTimeProvider implements TimePreferences.TimeProvider {

    private long elapsed;

    private long calendar;

    @Override
    public long getElapsedTime() {
        return elapsed;
    }

    @Override
    public long getCalendarTime() {
        return calendar;
    }

    public void setTime(long elapsed, long calendar) {
        this.elapsed = elapsed;
        this.calendar = calendar;
    }
}

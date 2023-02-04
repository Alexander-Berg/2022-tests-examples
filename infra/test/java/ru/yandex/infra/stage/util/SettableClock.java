package ru.yandex.infra.stage.util;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

public class SettableClock extends Clock {
    private Instant instant;

    public SettableClock() {
        this.instant = Instant.ofEpochSecond(0);
    }

    @Override
    public ZoneId getZone() {
        return ZoneId.systemDefault();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return instant;
    }

    public void incrementSecond() {
        moveTime(Duration.ofSeconds(1));
    }

    public void moveTime(Duration duration) {
        instant = instant.plus(duration);
    }
}

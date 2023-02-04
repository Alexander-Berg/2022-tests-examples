package ru.yandex.solomon.ut;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.base.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Vladimir Gordiychuk
 */
public class ManualClock extends Clock {
    private static final Logger logger = LoggerFactory.getLogger(ManualClock.class);

    private final Clock base = Clock.systemUTC();
    private final AtomicLong deltaMillis = new AtomicLong();
    private final List<Runnable> listeners = new ArrayList<>();

    public ManualClock() {
    }

    public synchronized void passedTime(long value, TimeUnit unit) {
        deltaMillis.addAndGet(unit.toMillis(value));
        logger.debug("Changed time, now {}", instant());
        listeners.forEach(Runnable::run);
    }

    @Override
    public ZoneId getZone() {
        return base.getZone();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException("Unsupported");
    }

    @Override
    public long millis() {
        return base.millis() + deltaMillis.get();
    }

    @Override
    public Instant instant() {
        return Instant.ofEpochMilli(base.millis() + deltaMillis.get());
    }

    public Ticker asTicker() {
        return new Ticker() {
            @Override
            public long read() {
                return System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(deltaMillis.get());
            }
        };
    }

    public synchronized void onShiftListener(Runnable listener) {
        listeners.add(listener);
    }
}

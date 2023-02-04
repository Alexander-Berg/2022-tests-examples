package ru.yandex.solomon.ut;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.Ticker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class ManualTicker extends Ticker {
    private final AtomicLong offsetNanos = new AtomicLong(0);
    private static final Logger logger = LoggerFactory.getLogger(ManualTicker.class);

    public void passedTime(long count, TimeUnit unit) {
        offsetNanos.addAndGet(unit.toNanos(count));
        logger.debug("Time was advanced by +{}ns", unit.toNanos(count));
    }

    @Override
    public long read() {
        return Ticker.systemTicker().read() + offsetNanos.get();
    }
}

package ru.yandex.payments.fnsreg;

import java.time.Duration;

import com.hazelcast.internal.util.Clock.ClockImpl;
import com.hazelcast.internal.util.ClockProperties;

public class HazelcastTimeTravelClock extends ClockImpl {
    private static long timePoint = 0;

    @Override
    protected long currentTimeMillis() {
        return timePoint;
    }

    /**
     * Method activates time travel clock for hazelcast. Needs to be executed within test's `static` block.
     */
    static void install() {
        System.setProperty(ClockProperties.HAZELCAST_CLOCK_IMPL, HazelcastTimeTravelClock.class.getName());
    }

    static void moveBackward(Duration offset) {
        timePoint -= offset.toMillis();
    }

    static void moveForward(Duration offset) {
        timePoint += offset.toMillis();
    }
}

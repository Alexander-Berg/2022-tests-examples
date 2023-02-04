package ru.yandex.intranet.d.tms;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Stub Hourglass scheduler launcher interface.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@Component
@Profile({"integration-tests"})
public class StubHourglassSchedulerStarter implements HourglassSchedulerStarter {

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public long getRetryCount() {
        return 0;
    }

}

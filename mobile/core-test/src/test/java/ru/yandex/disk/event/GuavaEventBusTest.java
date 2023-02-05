package ru.yandex.disk.event;

import com.google.common.eventbus.Subscribe;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import ru.yandex.disk.util.Diagnostics;
import ru.yandex.disk.util.Executors2;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;

@RunWith(RobolectricTestRunner.class)
public class GuavaEventBusTest implements EventListener {

    private int invocationCounter;

    private static class TestEvent extends Event {
    }

    @Test
    public void shouldNotifySeveralTimesForSameInstance() throws Exception {
        GuavaEventBus eventBus = new GuavaEventBus(Executors2.RUN_IMMEDIATELY_EXECUTOR,
                mock(Diagnostics.class));

        eventBus.registerListener(this);

        TestEvent event = new TestEvent();
        eventBus.send(event);
        eventBus.send(event);

        assertThat(invocationCounter, equalTo(2));
    }

    @Subscribe
    public void on(TestEvent e) {
        invocationCounter++;
    }
}
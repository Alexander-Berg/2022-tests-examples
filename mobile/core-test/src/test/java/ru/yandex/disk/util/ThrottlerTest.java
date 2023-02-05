package ru.yandex.disk.util;

import android.os.Handler;
import org.junit.Test;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import ru.yandex.disk.test.AndroidTestCase2;
import rx.functions.Func0;

import java.util.concurrent.CountDownLatch;

import static org.mockito.Mockito.*;

@Config(manifest = Config.NONE)
public class ThrottlerTest extends AndroidTestCase2 {

    private Throttler throttler;
    private Handler handler;
    private long throttleTimeMs;
    private Func0 targetObject;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        handler = spy(new Handler());

        throttleTimeMs = 100;
        targetObject = mock(Func0.class);

        throttler = new Throttler(handler, targetObject::call, throttleTimeMs);
    }

    @Test
    public void mustThrottleEvents() throws Exception {
        generateSeveralEvents();

        verifyHandlerAndTargetInvocation(1);
    }

    @Test
    public void mustThrottleFromManyThreads() throws Exception {
        final int threadCount = 10;
        final CountDownLatch latch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                generateSeveralEvents();
                latch.countDown();
            }).start();
        }
        latch.await();

        verifyHandlerAndTargetInvocation(1);
    }

    @Test
    public void mustThrottleSeveralTimes() throws Exception {
        generateSeveralEvents();
        verifyHandlerAndTargetInvocation(1);

        generateSeveralEvents();
        verifyHandlerAndTargetInvocation(2);
    }

    private void verifyHandlerAndTargetInvocation(final int times) {
        verify(handler, times(times)).postDelayed(any(), eq(throttleTimeMs));
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        verify(targetObject, times(times)).call();
    }

    private void generateSeveralEvents() {
        throttler.onEvent();
        throttler.onEvent();
        throttler.onEvent();
        throttler.onEvent();
    }
}

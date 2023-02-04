package ru.yandex.solomon.ut;

import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.ParametersAreNonnullByDefault;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.NANOSECONDS;

/**
 * @author Vladimir Gordiychuk
 */
@ParametersAreNonnullByDefault
public class ManualScheduledExecutorService extends ScheduledThreadPoolExecutor {
    private final ManualClock clock;

    public ManualScheduledExecutorService(int corePoolSize, ManualClock clock) {
        super(corePoolSize);
        this.clock = clock;
        this.clock.onShiftListener(this::wakeup);
    }

    private static final Runnable WAKEUP_NOP = () -> {};

    private void wakeup() {
        schedule(WAKEUP_NOP, 1, TimeUnit.NANOSECONDS);
    }

    @Override
    protected <V> RunnableScheduledFuture<V> decorateTask(Callable<V> callable, RunnableScheduledFuture<V> task) {
        return new RunnableScheduledFutureTask<>(callable, clock, task);
    }

    @Override
    protected <V> RunnableScheduledFuture<V> decorateTask(Runnable runnable, RunnableScheduledFuture<V> task) {
        return new RunnableScheduledFutureTask<>(runnable, clock, task);
    }

    private static final class RunnableScheduledFutureTask<V> implements RunnableScheduledFuture<V> {
        private static final Logger logger = LoggerFactory.getLogger(RunnableScheduledFutureTask.class);

        private final Object task;
        private final RunnableScheduledFuture<V> future;
        private final ManualClock clock;
        private final AtomicLong timeMillis;

        private RunnableScheduledFutureTask(Object task, ManualClock clock, RunnableScheduledFuture<V> future) {
            this.clock = clock;
            this.future = future;
            this.timeMillis = new AtomicLong(clock.millis() + future.getDelay(TimeUnit.MILLISECONDS));
            this.task = task;
            if (task != WAKEUP_NOP) {
                logger.debug("Scheduled " + task + " to run at " + Instant.ofEpochMilli(timeMillis.get()));
            }
        }

        @Override
        public void run() {
            if (!isPeriodic()) {
                future.run();
                return;
            }

            long initDelay = future.getDelay(TimeUnit.MILLISECONDS);
            try {
                future.run();
            } finally {
                long updateDelay = future.getDelay(TimeUnit.MILLISECONDS);
                if (initDelay > 0) {
                    updateDelay -= initDelay;
                }
                timeMillis.addAndGet(updateDelay);
                logger.debug("Scheduled next " + task + " run at " + Instant.ofEpochMilli(timeMillis.get()));
            }
        }

        @Override
        public boolean isPeriodic() {
            return future.isPeriodic();
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(timeMillis.get() - clock.millis(), MICROSECONDS);
        }

        @Override
        public int compareTo(Delayed other) {
            if (other == this) {
                return 0;
            }

            if (other instanceof RunnableScheduledFutureTask<?> otherTask) {
                return Long.compare(timeMillis.get(), otherTask.timeMillis.get());
            }

            return Long.compare(getDelay(NANOSECONDS), other.getDelay(NANOSECONDS));
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return future.cancel(mayInterruptIfRunning);
        }

        @Override
        public boolean isCancelled() {
            return future.isCancelled();
        }

        @Override
        public boolean isDone() {
            return future.isDone();
        }

        @Override
        public V get() throws InterruptedException, ExecutionException {
            return future.get();
        }

        @Override
        public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return future.get(timeout, unit);
        }
    }
}

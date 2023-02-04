package ru.yandex.solomon.ut;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;

/**
 * @author Vladimir Gordiychuk
 */
public class ManualTimer implements Timer {
    private ManualScheduledExecutorService executorService;

    public ManualTimer(ManualClock clock) {
        executorService = new ManualScheduledExecutorService(1, clock);
    }

    @Override
    public Timeout newTimeout(TimerTask task, long delay, TimeUnit unit) {
        TimeoutImpl timeout = new TimeoutImpl(task);
        timeout.future = executorService.schedule(timeout, delay, unit);
        return timeout;
    }

    @Override
    public Set<Timeout> stop() {
        executorService.shutdownNow();
        return Collections.emptySet();
    }

    private class TimeoutImpl implements Timeout, Callable<Void> {
        private final TimerTask task;
        private volatile boolean canceled;
        private volatile boolean expired;
        private volatile ScheduledFuture<?> future;

        public TimeoutImpl(TimerTask task) {
            this.task = task;
        }

        @Override
        public Timer timer() {
            return ManualTimer.this;
        }

        @Override
        public TimerTask task() {
            return task;
        }

        @Override
        public boolean isExpired() {
            return expired;
        }

        @Override
        public boolean isCancelled() {
            return canceled;
        }

        @Override
        public boolean cancel() {
            if (!canceled) {
                canceled = true;
                if (future != null) {
                    future.cancel(false);
                }
                return true;
            }

            return false;
        }

        public Void call() throws Exception {
            if (canceled) {
                return null;
            }
            expired = true;
            task.run(this);
            return null;
        }
    }
}

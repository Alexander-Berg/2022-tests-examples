package com.yandex.maps.testapp.mrc.camera;

import android.util.Log;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class RepeatedTask {
    private static final String TAG = "RepeatedTask";

    private Runnable task;
    private long initialDelayMillis;
    private long periodMillis;

    private AtomicLong lastRunTime = new AtomicLong(0L);

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(/*poolSize=*/1);

    ScheduledFuture<?> future = null;

    public RepeatedTask(final Runnable userTask) {
        this.task = new Runnable() {
            @Override
            public void run() {
                try {
                    userTask.run();
                } catch (Exception e) {
                    Log.e(TAG, "Repeated task failed: " + e.getMessage());
                    e.printStackTrace();
                }
                lastRunTime.set(System.currentTimeMillis());
            }
        };
    }

    /**
     * Start executing task with the given periodMillis.
     * @param initialDelayMillis initial delay before first run
     * @param periodMillis task execution periodMillis
     */
    public void run(long initialDelayMillis, long periodMillis) {
        if (future != null) {
            throw new IllegalStateException("Task is already running");
        }

        this.initialDelayMillis = initialDelayMillis;
        this.periodMillis = periodMillis;

        future = scheduler.scheduleAtFixedRate(
                task, this.initialDelayMillis, this.periodMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * @return timestamp of last task run. Zero if task has never been run.
     */
    public long lastRunTimeMillis() {
        return lastRunTime.get();
    }

    /**
     * Stop repeating task execution and cancel currently executed task if any.
     */
    public void cancel() {
        if (future != null) {
            future.cancel(/*mayInterrupt=*/true);
            future = null;
        }
    }
}

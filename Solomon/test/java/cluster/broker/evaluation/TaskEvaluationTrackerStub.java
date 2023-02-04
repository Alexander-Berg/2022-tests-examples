package ru.yandex.solomon.alert.cluster.broker.evaluation;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import io.grpc.Status;

/**
 * @author Vladimir Gordiychuk
 */
public class TaskEvaluationTrackerStub<T> implements TaskEvaluationTracker<T> {
    public volatile long timeoutMillis = 10_000;
    public volatile CountDownLatch sync = new CountDownLatch(1);
    public ConcurrentHashMap<T, Boolean> complete = new ConcurrentHashMap<>();
    public ConcurrentHashMap<T, Boolean> warmup = new ConcurrentHashMap<>();
    public ConcurrentHashMap<T, Status> error = new ConcurrentHashMap<>();

    @Override
    public void onTaskComplete(T value) {
        complete.put(value, true);
        onEvent();
    }

    @Override
    public void onTaskCompleteWarmup(T value) {
        warmup.put(value, true);
        onEvent();
    }

    @Override
    public void onTaskError(T value, Status status) {
        error.put(value, status);
        onEvent();
    }

    @Override
    public long noMessageTimeoutMillis() {
        return timeoutMillis;
    }

    private void onEvent() {
        var copy = sync;
        sync = new CountDownLatch(1);
        copy.countDown();
    }
}

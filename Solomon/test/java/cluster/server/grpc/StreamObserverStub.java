package ru.yandex.solomon.alert.cluster.server.grpc;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import io.grpc.Status;
import io.grpc.stub.StreamObserver;


/**
 * @author Vladimir Gordiychuk
 */
public class StreamObserverStub<T> implements StreamObserver<T> {
    public final CompletableFuture<Status> done = new CompletableFuture<>();
    public final ArrayBlockingQueue<T> events = new ArrayBlockingQueue<>(10000);

    @Override
    public void onNext(T value) {
        events.add(value);
    }

    @Override
    public void onError(Throwable t) {
        done.complete(Status.fromThrowable(t));
    }

    @Override
    public void onCompleted() {
        done.complete(Status.OK);
    }

    public T takeEvent() {
        try {
            return events.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    public T poolEvent(long time, TimeUnit unit) {
        try {
            return events.poll(time, unit);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}

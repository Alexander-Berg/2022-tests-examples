package ru.yandex.solomon.alert.cluster.server.grpc.evaluation;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;

import io.grpc.Status;

import static org.junit.Assert.assertEquals;

/**
 * @author Vladimir Gordiychuk
 */
public class SubscriberStub<T> implements Flow.Subscriber<T> {
    public final ArrayBlockingQueue<T> events = new ArrayBlockingQueue<>(10000);
    public final CompletableFuture<Status> doneFuture = new CompletableFuture<>();
    public volatile Subscription subscription;

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = subscription;
        subscription.request(1);
    }

    @Override
    public void onNext(T item) {
        events.add(item);
        subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
        doneFuture.complete(Status.fromThrowable(throwable));
    }

    @Override
    public void onComplete() {
        doneFuture.complete(Status.OK);
    }

    public T takeEvent() {
        try {
            return events.take();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void expectNoEvents() {
        assertEquals("events queue size on subscriber should be zero", 0, events.size());
    }
}

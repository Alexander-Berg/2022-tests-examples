package ru.yandex.solomon.alert.cluster.broker.evaluation;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;

import io.grpc.Status;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Vladimir Gordiychuk
 */
public class EvaluationStreamObserverTest {
    private TaskEvaluationTrackerStub<EvaluationStreamObserver> tracker;
    private ExecutorService executor = ForkJoinPool.commonPool();

    @Before
    public void setUp() throws Exception {
        tracker = new TaskEvaluationTrackerStub<>();
    }

    @Test
    public void cancelSubscriptionOnCancel() throws InterruptedException {
        var eval = new EvaluationTaskStub();
        var observer = observer(eval);

        var sync = eval.sync;
        var subscription = new Subscription();
        observer.onSubscribe(subscription);

        assertFalse(subscription.canceled);
        observer.cancel();

        sync.await();
        assertTrue(subscription.canceled);
        assertEquals(Status.Code.CANCELLED, eval.latestStatus.getCode());
        assertFalse(eval.task.isCanceled());
    }

    @Test
    public void timeout() throws InterruptedException {
        tracker.timeoutMillis = 1;
        var eval = new EvaluationTaskStub();
        var sync = eval.sync;

        var observer = observer(eval);
        var subscription = new Subscription();
        observer.onSubscribe(subscription);

        sync.await();
        assertEquals(Status.Code.DEADLINE_EXCEEDED, eval.latestStatus.getCode());
        subscription.cancelSync.await();
        assertTrue(subscription.canceled);
    }

    @Test
    public void onWarmupReport() throws InterruptedException {
        var eval = new EvaluationTaskStub();

        var observer = observer(eval);
        var subscription = new Subscription();
        observer.onSubscribe(subscription);

        // report about warmup
        {
            assertFalse(tracker.warmup.getOrDefault(observer, false));

            nextCall(observer, eval).join();

            assertTrue(tracker.warmup.getOrDefault(observer, false));
            assertNotNull(eval.latestEvaluation);
        }

        // avoid repeat complete warmup
        tracker.warmup.clear();
        for (int index = 0; index < 10; index++) {
            var syncEval = eval.sync;
            nextCall(observer, eval);
            syncEval.await();
        }
        assertFalse(tracker.warmup.getOrDefault(observer, false));
    }

    @Test
    public void reportNextCall() {
        var eval = new EvaluationTaskStub();

        var observer = observer(eval);
        observer.onSubscribe(new Subscription());

        var prevEval = eval.latestEvaluation;
        for (int index = 0; index < 10; index++) {
            nextCall(observer, eval).join();
            assertNotEquals(prevEval, eval.latestEvaluation);
            prevEval = eval.latestEvaluation;
        }
    }

    private CompletableFuture<Void> nextCall(EvaluationStreamObserver observer, EvaluationTaskStub eval) {
        return CompletableFuture.runAsync(() -> observer.onNext(eval.nextEval()), executor);
    }

    private EvaluationStreamObserver observer(EvaluationTaskStub eval) {
        return new EvaluationStreamObserver(eval.task, tracker);
    }

    private static class Subscription implements Flow.Subscription {
        private volatile boolean canceled;
        private final CountDownLatch cancelSync = new CountDownLatch(1);

        @Override
        public void request(long n) {
        }

        @Override
        public void cancel() {
            canceled = true;
            cancelSync.countDown();
        }
    }
}

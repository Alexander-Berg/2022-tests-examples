package ru.yandex.infra.stage.concurrent;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.base.Throwables;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.backoff.FixedBackOff;

import ru.yandex.infra.controller.metrics.MapGaugeRegistry;
import ru.yandex.infra.controller.util.ExceptionUtils;
import ru.yandex.infra.stage.util.StoppableExponentialBackOff;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.infra.controller.testutil.FutureUtils.get1s;

public class SerialExecutorTest {

    private SerialExecutor serialExecutor;
    private MapGaugeRegistry metrics;

    @BeforeEach
    void before() {
        metrics = new MapGaugeRegistry();
        serialExecutor = new SerialExecutor(getClass().getName(), metrics);
    }

    @AfterEach
    void after() {
        serialExecutor.shutdown();
    }

    @Test
    void getAllSubmittedFutures() {
        var f1 = new CompletableFuture<>();
        var f2 = new CompletableFuture<>();

        var sf1 = serialExecutor.submitFuture(f1, x -> {}, x -> {});
        var sf2 = serialExecutor.submitFuture(f2, x -> {}, x -> {});

        f1.completeExceptionally(new RuntimeException());

        var futures = serialExecutor.getAllSubmittedFutures();
        assertEquals(2, futures.size());
        assertSame(sf1, futures.get(0));
        assertSame(sf2, futures.get(1));

        futures = serialExecutor.getAllSubmittedFutures();
        assertEquals(0, futures.size());

        var sf3 = serialExecutor.submitFuture(f1, x -> {}, x -> {});
        futures = serialExecutor.getAllSubmittedFutures();
        assertEquals(1, futures.size());
        assertSame(sf3, futures.get(0));
    }

    @Test
    void futureMetrics() {
        assertEquals(0, metrics.getGaugeValue(SerialExecutor.METRIC_ACTIVE_FUTURES));
        assertEquals(0L, metrics.getGaugeValue(SerialExecutor.METRIC_SUBMITTED_FUTURES_TOTAL));
        assertEquals(0L, metrics.getGaugeValue(SerialExecutor.METRIC_FAILED_FUTURES));

        var f1 = new CompletableFuture<>();
        var sf1 = serialExecutor.submitFuture(f1, x -> {}, x -> {});
        var f2 = new CompletableFuture<>();
        var sf2 = serialExecutor.submitFuture(f2, x -> {}, x -> {});

        var sf3 = serialExecutor.submitFuture(CompletableFuture.completedFuture(null), x -> {}, x -> {});
        var sf4 = serialExecutor.submitFuture(CompletableFuture.failedFuture(new RuntimeException()), x -> {}, x -> {});

        get1s(sf3);
        assertThrows(RuntimeException.class, () -> get1s(sf4));

        assertEquals(2, metrics.getGaugeValue(SerialExecutor.METRIC_ACTIVE_FUTURES));
        assertEquals(4L, metrics.getGaugeValue(SerialExecutor.METRIC_SUBMITTED_FUTURES_TOTAL));
        assertEquals(1L, metrics.getGaugeValue(SerialExecutor.METRIC_FAILED_FUTURES));

        f1.complete(null);
        get1s(sf1);

        assertEquals(1, metrics.getGaugeValue(SerialExecutor.METRIC_ACTIVE_FUTURES));
        assertEquals(4L, metrics.getGaugeValue(SerialExecutor.METRIC_SUBMITTED_FUTURES_TOTAL));
        assertEquals(1L, metrics.getGaugeValue(SerialExecutor.METRIC_FAILED_FUTURES));

        f2.completeExceptionally(new RuntimeException());
        assertThrows(RuntimeException.class, () -> get1s(sf2));

        assertEquals(0, metrics.getGaugeValue(SerialExecutor.METRIC_ACTIVE_FUTURES));
        assertEquals(4L, metrics.getGaugeValue(SerialExecutor.METRIC_SUBMITTED_FUTURES_TOTAL));
        assertEquals(2L, metrics.getGaugeValue(SerialExecutor.METRIC_FAILED_FUTURES));

        serialExecutor.submitFuture(new CompletableFuture<>(), x -> {}, x -> {});
        var sf5 = serialExecutor.submitFuture(CompletableFuture.completedFuture(null), x -> {}, x -> {});
        get1s(sf5);
        assertEquals(1, metrics.getGaugeValue(SerialExecutor.METRIC_ACTIVE_FUTURES));
        assertEquals(6L, metrics.getGaugeValue(SerialExecutor.METRIC_SUBMITTED_FUTURES_TOTAL));
        assertEquals(2L, metrics.getGaugeValue(SerialExecutor.METRIC_FAILED_FUTURES));
    }

    @Test
    void actionMetrics() throws ExecutionException, InterruptedException {
        assertEquals(0, metrics.getGaugeValue(SerialExecutor.METRIC_ACTIVE_ACTIONS));
        assertEquals(0L, metrics.getGaugeValue(SerialExecutor.METRIC_SUBMITTED_ACTIONS_TOTAL));
        assertEquals(0L, metrics.getGaugeValue(SerialExecutor.METRIC_FAILED_ACTIONS));

        var f1 = new CompletableFuture<>();
        var sf1 = serialExecutor.schedule(() -> {
            try {
                f1.get();
            } catch (InterruptedException|ExecutionException ignore) {}
        }, Duration.ZERO);

        var f2 = new CompletableFuture<>();
        var sf2 = serialExecutor.schedule(() -> {
            try {
                f2.get();
            } catch (InterruptedException|ExecutionException e) {
                Throwable stripped = ExceptionUtils.stripCompletionException(e);
                Throwables.throwIfUnchecked(stripped);
            }
        }, Duration.ZERO);


        assertEquals(2, metrics.getGaugeValue(SerialExecutor.METRIC_ACTIVE_ACTIONS));
        assertEquals(2L, metrics.getGaugeValue(SerialExecutor.METRIC_SUBMITTED_ACTIONS_TOTAL));
        assertEquals(0L, metrics.getGaugeValue(SerialExecutor.METRIC_FAILED_ACTIONS));

        f1.complete(null);
        sf1.get();

        assertEquals(1, metrics.getGaugeValue(SerialExecutor.METRIC_ACTIVE_ACTIONS));
        assertEquals(2L, metrics.getGaugeValue(SerialExecutor.METRIC_SUBMITTED_ACTIONS_TOTAL));
        assertEquals(0L, metrics.getGaugeValue(SerialExecutor.METRIC_FAILED_ACTIONS));

        f2.completeExceptionally(new RuntimeException());
        sf2.get();

        assertEquals(0, metrics.getGaugeValue(SerialExecutor.METRIC_ACTIVE_ACTIONS));
        assertEquals(2L, metrics.getGaugeValue(SerialExecutor.METRIC_SUBMITTED_ACTIONS_TOTAL));
        assertEquals(1L, metrics.getGaugeValue(SerialExecutor.METRIC_FAILED_ACTIONS));
    }

    static class TestException extends Error {}

    @Test
    void callbackExeptionShouldBeIgnored() {
        var sf1 = serialExecutor.submitFuture(CompletableFuture.completedFuture(null), x -> {
            throw new RuntimeException("Error in success callback");
        }, x -> {});

        var sf2 = serialExecutor.submitFuture(CompletableFuture.failedFuture(new TestException()), x -> {}, x -> {
            throw new RuntimeException("Error in failed future callback");
        });

        get1s(sf1);
        assertThrows(TestException.class, () -> get1s(sf2));

        assertEquals(0, metrics.getGaugeValue(SerialExecutor.METRIC_ACTIVE_FUTURES));
        assertEquals(2L, metrics.getGaugeValue(SerialExecutor.METRIC_SUBMITTED_FUTURES_TOTAL));
        assertEquals(1L, metrics.getGaugeValue(SerialExecutor.METRIC_FAILED_FUTURES));
        assertEquals(2L, metrics.getGaugeValue(SerialExecutor.METRIC_FAILED_ACTIONS));
    }

    @Test
    void executeOrRetryNoExceptions() {
        var f1 = new CompletableFuture<>();
        var sf1 = serialExecutor.executeOrRetry(() -> f1, x -> {}, x -> {}, new FixedBackOff().start());

        assertEquals(1, metrics.getGaugeValue(SerialExecutor.METRIC_ACTIVE_FUTURES));
        assertEquals(1L, metrics.getGaugeValue(SerialExecutor.METRIC_SUBMITTED_FUTURES_TOTAL));
        assertEquals(0L, metrics.getGaugeValue(SerialExecutor.METRIC_FAILED_FUTURES));

        f1.complete(null);
        get1s(sf1);

        assertEquals(0, metrics.getGaugeValue(SerialExecutor.METRIC_ACTIVE_FUTURES));
        assertEquals(1L, metrics.getGaugeValue(SerialExecutor.METRIC_SUBMITTED_FUTURES_TOTAL));
        assertEquals(0L, metrics.getGaugeValue(SerialExecutor.METRIC_FAILED_FUTURES));
        assertEquals(0L, metrics.getGaugeValue(SerialExecutor.METRIC_FAILED_FUTURES_RETRIES));
    }

    @Test
    void executeOrRetry() {
        var counter = new AtomicLong();
        long expectedFailures = 5;

        var sf1 = serialExecutor.executeOrRetry(() -> {
            if(counter.incrementAndGet() <= expectedFailures) {
                return CompletableFuture.failedFuture(new RuntimeException("Failed attempt" + counter.get()));
            }
            return CompletableFuture.completedFuture(null);
        }, x -> {}, x -> {}, new FixedBackOff(1, 100).start());

        get1s(sf1);

        assertEquals(0, metrics.getGaugeValue(SerialExecutor.METRIC_ACTIVE_FUTURES));
        assertEquals(expectedFailures + 1, metrics.getGaugeValue(SerialExecutor.METRIC_SUBMITTED_FUTURES_TOTAL));
        assertEquals(expectedFailures, metrics.getGaugeValue(SerialExecutor.METRIC_FAILED_FUTURES));
        assertEquals(expectedFailures, metrics.getGaugeValue(SerialExecutor.METRIC_FAILED_FUTURES_RETRIES));
    }

    @Test
    void executeOrRetryManualStop() {
        var counter = new AtomicLong();
        var backoff = new StoppableExponentialBackOff(1, 2).startStoppable();
        long expectedFailures = 5;
        var sf1 = serialExecutor.executeOrRetry(() -> {
            if(counter.incrementAndGet() == expectedFailures) {
                backoff.stop();
            }
            return CompletableFuture.failedFuture(new RuntimeException());
        }, x -> {}, x -> {}, backoff);

        get1s(sf1);

        assertEquals(0, metrics.getGaugeValue(SerialExecutor.METRIC_ACTIVE_FUTURES));
        assertEquals(expectedFailures, metrics.getGaugeValue(SerialExecutor.METRIC_SUBMITTED_FUTURES_TOTAL));
        assertEquals(expectedFailures, metrics.getGaugeValue(SerialExecutor.METRIC_FAILED_FUTURES));
        assertEquals(expectedFailures-1, metrics.getGaugeValue(SerialExecutor.METRIC_FAILED_FUTURES_RETRIES));
    }

    @Test
    void shouldNotFailForAbsentCallback() {
        var sf1 = serialExecutor.submitFuture(CompletableFuture.completedFuture(null), null, x -> {});
        var sf2 = serialExecutor.submitFuture(CompletableFuture.failedFuture(new TestException()), x -> {}, null);
        var sf3 = serialExecutor.submitFuture(CompletableFuture.completedFuture(null), null, null);
        var sf4 = serialExecutor.submitFuture(CompletableFuture.failedFuture(new TestException()), null, null);

        get1s(sf1);
        assertThrows(TestException.class, () -> get1s(sf2));
        get1s(sf3);
        assertThrows(TestException.class, () -> get1s(sf4));

        assertEquals(0L, metrics.getGaugeValue(SerialExecutor.METRIC_FAILED_ACTIONS));

        assertEquals(0, metrics.getGaugeValue(SerialExecutor.METRIC_ACTIVE_FUTURES));
        assertEquals(4L, metrics.getGaugeValue(SerialExecutor.METRIC_SUBMITTED_FUTURES_TOTAL));
        assertEquals(2L, metrics.getGaugeValue(SerialExecutor.METRIC_FAILED_FUTURES));
    }

}

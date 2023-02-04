package ru.yandex.infra.controller;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.infra.controller.metrics.MapGaugeRegistry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.infra.controller.testutil.FutureUtils.get1s;
import static ru.yandex.infra.controller.testutil.FutureUtils.get5s;

public class RepeatedTaskTest {

    private static final Logger LOG = LoggerFactory.getLogger(RepeatedTaskTest.class);

    ScheduledExecutorService executor;
    private MapGaugeRegistry gaugeRegistry;
    private RepeatedTask startedRepeadedTask;

    @BeforeEach
    void before() {
        executor = Executors.newSingleThreadScheduledExecutor(runnable -> new Thread(runnable, "testthread"));
        gaugeRegistry = new MapGaugeRegistry();
    }

    @AfterEach
    void after() {
        startedRepeadedTask.stop();
        executor.shutdownNow();
    }

    static class TaskResults {
        public int iterations;
        public boolean executing;
    }

    private RepeatedTask create(Runnable task, Duration updateInterval) {
        return create(task, updateInterval, Duration.ofDays(1));
    }

    private RepeatedTask create(Runnable task, Duration updateInterval, Duration singleIterationTimeout) {
        startedRepeadedTask = new RepeatedTask(() -> CompletableFuture.runAsync(task),
                updateInterval,
                singleIterationTimeout,
                executor,
                Optional.of(gaugeRegistry),
                LOG,
                true);
        return startedRepeadedTask;
    }

    @Test
    void singleRun() {
        CompletableFuture<?> future = new CompletableFuture<>();
        var task = create(() -> future.complete(null), Duration.ofMillis(10));
        task.start();
        get5s(future);
        assertEquals(0L, gaugeRegistry.getGaugeValue(RepeatedTask.METRIC_FAILED_ITERATIONS_COUNT));
        assertEquals(0L, gaugeRegistry.getGaugeValue(RepeatedTask.METRIC_HUNG_ITERATIONS_COUNT));
        assertNotNull(gaugeRegistry.getGaugeValue(RepeatedTask.METRIC_CURRENT_ITERATION_DURATION_TIME));
    }

    @Test
    void dontRunWithoutStart() {
        CompletableFuture<?> future = new CompletableFuture<>();
        create(() -> future.complete(null), Duration.ofMillis(10));

        assertThrows(RuntimeException.class, () -> get1s(future));

        assertEquals(0L, gaugeRegistry.getGaugeValue(RepeatedTask.METRIC_FINISHED_ITERATIONS_COUNT));
        assertNull(gaugeRegistry.getGaugeValue(RepeatedTask.METRIC_CURRENT_ITERATION_DURATION_TIME));
    }

    @Test
    void multipleRuns() {
        long requiredNumberOfIterations = 10;
        TaskResults results = new TaskResults();
        CompletableFuture<?> future = new CompletableFuture<>();
        var task = create(() -> {
            if (results.iterations == requiredNumberOfIterations) {
                future.complete(null);
            }
            results.iterations++;
        }, Duration.ofMillis(10));
        task.start();

        get5s(future);

        assertThat((Long)gaugeRegistry.getGaugeValue(RepeatedTask.METRIC_FINISHED_ITERATIONS_COUNT), greaterThanOrEqualTo(requiredNumberOfIterations));
    }

    @Test
    void rescheduleAfterFailures() {

        long requiredNumberOfIterations = 10;
        TaskResults results = new TaskResults();
        CompletableFuture<?> future = new CompletableFuture<>();
        var task = create(() -> {
            if (results.iterations == requiredNumberOfIterations) {
                future.complete(null);
            }
            results.iterations++;
            throw new RuntimeException("Test exception");
        }, Duration.ofMillis(10));
        task.start();

        get5s(future);

        assertThat((Long)gaugeRegistry.getGaugeValue(RepeatedTask.METRIC_FINISHED_ITERATIONS_COUNT), greaterThanOrEqualTo(requiredNumberOfIterations));
        assertThat((Long)gaugeRegistry.getGaugeValue(RepeatedTask.METRIC_FAILED_ITERATIONS_COUNT), greaterThanOrEqualTo(requiredNumberOfIterations));
        assertEquals(0L, gaugeRegistry.getGaugeValue(RepeatedTask.METRIC_HUNG_ITERATIONS_COUNT));
    }

    @Test
    void rescheduleAfterHang() {

        long requiredNumberOfIterations = 3;
        TaskResults results = new TaskResults();
        CompletableFuture<?> future = new CompletableFuture<>();
        var task = create(() -> {
            if (results.iterations == requiredNumberOfIterations) {
                future.complete(null);
            }
            results.iterations++;
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {}
        }, Duration.ofMillis(10), Duration.ofMillis(100));
        task.start();

        get5s(future);

        assertThat((Long)gaugeRegistry.getGaugeValue(RepeatedTask.METRIC_FINISHED_ITERATIONS_COUNT), greaterThanOrEqualTo(requiredNumberOfIterations));
        assertThat((Long)gaugeRegistry.getGaugeValue(RepeatedTask.METRIC_FAILED_ITERATIONS_COUNT), greaterThanOrEqualTo(requiredNumberOfIterations));
        assertThat((Long)gaugeRegistry.getGaugeValue(RepeatedTask.METRIC_HUNG_ITERATIONS_COUNT), greaterThanOrEqualTo(requiredNumberOfIterations));
    }

    @Test
    void currentIterationDurationMetric() {
        CompletableFuture<?> future = new CompletableFuture<>();
        var task = create(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {}
            future.complete(null);
            try {
                Thread.sleep(100000);
            } catch (InterruptedException ignored) {}
        }, Duration.ofMillis(1));

        task.start();
        get5s(future);
        assertEquals(0L, gaugeRegistry.getGaugeValue(RepeatedTask.METRIC_FINISHED_ITERATIONS_COUNT));
        assertThat((Long)gaugeRegistry.getGaugeValue(RepeatedTask.METRIC_CURRENT_ITERATION_DURATION_TIME), greaterThanOrEqualTo(1000L));
    }

    @Test
    void dontScheduleNextIfTaskIsNotReadyYet() {

        CompletableFuture<?> future = new CompletableFuture<>();
        TaskResults results = new TaskResults();
        var task = create(() -> {
            assertFalse(results.executing);
            results.executing = true;
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}
            results.executing = false;
        }, Duration.ofMillis(1));

        task.start();

        //Waiting for 1 second
        assertThrows(RuntimeException.class, () -> get1s(future));

        //Expecting ~10 successful iterations
        assertThat((Long)gaugeRegistry.getGaugeValue(RepeatedTask.METRIC_FINISHED_ITERATIONS_COUNT), greaterThanOrEqualTo(5L));
        assertThat((Long)gaugeRegistry.getGaugeValue(RepeatedTask.METRIC_FINISHED_ITERATIONS_COUNT), lessThanOrEqualTo(15L));
    }

}

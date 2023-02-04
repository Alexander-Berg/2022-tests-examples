package ru.yandex.solomon.alert.unroll;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.monlib.metrics.labels.LabelsBuilder;
import ru.yandex.solomon.alert.domain.Alert;

/**
 * @author Vladimir Gordiychuk
 */
public class UnrollExecutorStub implements UnrollExecutor {
    private static final Logger logger = LoggerFactory.getLogger(UnrollExecutorStub.class);

    private final ScheduledExecutorService executorService;
    private final Map<String, Supplier<UnrollResult>> supplierById = new ConcurrentHashMap<>();
    private final Map<Map.Entry<String, Integer>, Semaphore> syncEval = new ConcurrentHashMap<>();

    public UnrollExecutorStub(ScheduledExecutorService executorService) {
        this.executorService = executorService;
    }

    public void predefineUnroll(String id, Supplier<Set<Labels>> supplier) {
        this.supplierById.put(id, () -> UnrollResult.of(supplier.get()));
    }

    public void predefineUnrollResult(String id, Supplier<UnrollResult> supplier) {
        this.supplierById.put(id, supplier);
    }

    public void predefineUnroll(String id, Set<Labels> labels) {
        this.supplierById.put(id, () -> UnrollResult.of(labels));
    }

    public Semaphore getSyncEval(String alertId, int version) {
        return syncEval.computeIfAbsent(Map.entry(alertId, version), (ignore) -> new Semaphore(0));
    }

    @Override
    public void unroll(Alert alert, UnrollConsumer consumer) {
        Task task = new Task(alert, null, consumer);
        schedule(task);
    }

    @Override
    public void unrollNow(Alert alert, UnrollConsumer consumer) {
        Task task = new Task(alert, null, consumer);
        schedule(task, 0);
    }

    @Override
    public void close() {
    }

    private void schedule(Task task) {
        schedule(task, TimeUnit.MINUTES.toMillis(1));
    }

    private void schedule(Task task, long delay) {
        executorService.schedule(task, delay, TimeUnit.MILLISECONDS);
    }

    private class Task implements Runnable {
        private final Alert alert;
        private final UnrollResult prev;
        private final UnrollConsumer consumer;

        public Task(Alert alert, UnrollResult prev, UnrollConsumer consumer) {
            this.alert = alert;
            this.prev = prev;
            this.consumer = consumer;
        }

        @Override
        public String toString() {
            return "UnrollExecutorStub$Task{" +
                    "alertId=" + alert.getId() +
                    ", prev=" + prev +
                    ", consumer=" + consumer +
                    '}';
        }

        @Override
        public void run() {
            logger.debug("Run unroll {}", this);
            try {
                if (consumer.isCanceled()) {
                    return;
                }

                UnrollResult next = supplierById.getOrDefault(alert.getId(), this::defaultUnrolling).get();
                if (!Objects.equals(prev, next)) {
                    logger.debug("Supplying alert {} with new unroll result {}", alert.getId(), next);
                    consumer.accept(next);
                }
                getSyncEval(alert.getId(), alert.getVersion()).release();
                Task nextTask = new Task(alert, prev, consumer);
                schedule(nextTask);
            } catch (RejectedExecutionException e) {
                // ignore
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }

        private UnrollResult defaultUnrolling() {
            List<String> labels = alert.getGroupByLabels();
            Set<Labels> result = new HashSet<>();
            for (int index = 0; index < 3; index++) {
                LabelsBuilder builder = Labels.builder(labels.size());
                for (String label : labels) {
                    builder.add(label, label + " - " + index);
                }
                result.add(builder.build());
            }

            return new UnrollResult(result, true);
        }
    }
}

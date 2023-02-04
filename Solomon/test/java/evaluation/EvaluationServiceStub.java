package ru.yandex.solomon.alert.evaluation;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.domain.AlertKey;
import ru.yandex.solomon.alert.rule.EvaluationState;
import ru.yandex.solomon.ut.ManualClock;

/**
 * @author Vladimir Gordiychuk
 */
public class EvaluationServiceStub implements EvaluationService {
    private static final Logger logger = LoggerFactory.getLogger(EvaluationServiceStub.class);

    private final ManualClock clock;
    private final ScheduledExecutorService executorService;
    private final ConcurrentMap<AlertKey, Task> taskByKey = new ConcurrentHashMap<>();

    public EvaluationServiceStub(ManualClock clock, ScheduledExecutorService executorService) {
        this.clock = clock;
        this.executorService = executorService;
    }

    @Override
    public void assign(Alert alert, @Nullable EvaluationState state, Consumer consumer) {
        if (state == null) {
            state = EvaluationState.newBuilder(alert)
                    .setSince(clock.instant())
                    .setStatus(EvaluationStatus.NO_DATA)
                    .build();
        }

        Task task = new Task(alert, consumer, state);
        Task prev = taskByKey.put(alert.getKey(), task);
        if (prev != null) {
            prev.cancel();
        }

        evalTask(task);
    }

    private void evalTask(Task task) {
        synchronized (clock) {
            long delay = task.nextTime().toEpochMilli() - clock.millis();
            logger.debug("{} will be executed after {} millis at {}",
                    task.alert.getKey(),
                    delay,
                    clock.instant().plusMillis(delay));

            executorService.schedule(() -> {
                if (!task.isCanceled()) {
                    task.evaluate();
                    evalTask(task);
                }
            }, delay, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void unassign(AlertKey key) {
        Task task = taskByKey.get(key);
        if (task != null) {
            task.cancel();
        }
    }

    @Override
    public Statistics statistics() {
        return new Statistics(taskByKey.size(), 0);
    }

    public boolean hasTask(AlertKey key) {
        var task = taskByKey.get(key);
        return task != null && !task.isCanceled();
    }

    @ParametersAreNonnullByDefault
    private static class Task {
        private final Alert alert;
        private final Consumer consumer;
        private volatile EvaluationState state;
        private volatile boolean canceled;

        Task(Alert alert, Consumer consumer, EvaluationState state) {
            this.alert = alert;
            this.consumer = consumer;
            this.state = state;
        }

        synchronized void evaluate() {
            EvaluationState prev = state;
            EvaluationState next = prev.nextStatus(EvaluationStatus.OK, prev.getLatestEval().plus(1, ChronoUnit.MINUTES));
            state = next;
            consumer.consume(next);
        }

        Instant nextTime() {
            return state.getLatestEval().plus(1, ChronoUnit.MINUTES);
        }

        boolean isCanceled() {
            return canceled || consumer.isCanceled();
        }

        synchronized void cancel() {
            canceled = true;
            consumer.onComplete();
        }
    }
}

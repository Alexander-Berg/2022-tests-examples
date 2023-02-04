package ru.yandex.solomon.alert.cluster.broker.evaluation;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.cluster.project.ProjectAssignment;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.protobuf.THeartbeatResponse;
import ru.yandex.solomon.alert.rule.EvaluationState;
import ru.yandex.solomon.ut.ManualClock;

/**
 * @author Vladimir Gordiychuk
 */
public class EvaluationAssignmentServiceStub implements EvaluationAssignmentService {
    private static final long EVAL_INTERVAL_MILLIS = TimeUnit.MINUTES.toMillis(1L);
    private static final Logger logger = LoggerFactory.getLogger(EvaluationAssignmentServiceStub.class);

    private final ManualClock clock;
    private final ScheduledExecutorService executorService;
    private final Map<String, Supplier<EvaluationStatus>> supplierById = new ConcurrentHashMap<>();
    private final Map<String, Semaphore> syncEval = new ConcurrentHashMap<>();

    public EvaluationAssignmentServiceStub(ManualClock clock, ScheduledExecutorService executorService) {
        this.clock = clock;
        this.executorService = executorService;
    }

    public void predefineStatus(String id, Supplier<EvaluationStatus> supplier) {
        this.supplierById.put(id, supplier);
    }

    public Semaphore getSyncEval(String alertId) {
        return syncEval.computeIfAbsent(alertId, (ignore) -> new Semaphore(0));
    }

    @Override
    public void update(THeartbeatResponse heartbeat) {
    }

    @Override
    public void assign(ProjectAssignment assignment, Alert alert, EvaluationObserver observer) {
        EvaluationState state = observer.getLatestEvaluation();
        Instant evalTime = getEvalTime(state);
        EvaluationSubscription subscription = new EvaluationSubscription(assignment, alert, observer);
        observer.onSubscribe(subscription);
        Task nextTask = new Task(subscription, state, evalTime);
        schedule(nextTask);
    }

    @Override
    public void close() {
    }

    private Instant getEvalTime(@Nullable EvaluationState state) {
        if (state != null) {
            return state.getLatestEval().plusMillis(EVAL_INTERVAL_MILLIS);
        } else {
            long randomDelay = ThreadLocalRandom.current().nextLong(0, EVAL_INTERVAL_MILLIS);
            return clock.instant().plusMillis(randomDelay);
        }
    }

    private void schedule(Task task) {
        synchronized (clock) {
            long delay = task.evalTime.toEpochMilli() - clock.millis();
            executorService.schedule(task, delay, TimeUnit.MILLISECONDS);
        }
    }

    private class Task implements Runnable {
        private final EvaluationSubscription subscription;
        private final EvaluationState state;
        private final Instant evalTime;

        Task(EvaluationSubscription subscription, EvaluationState state, Instant evalTime) {
            this.subscription = subscription;
            this.state = state;
            this.evalTime = evalTime;
        }

        @Override
        public String toString() {
            return "EvaluationAssignmentServiceStub$Task{" +
                    "alertKey=" + subscription.alert.getKey() +
                    ", evalTime=" + evalTime +
                    '}';
        }

        @Override
        public void run() {
            try {
                if (subscription.canceled) {
                    return;
                }

                EvaluationStatus status =
                        supplierById.getOrDefault(subscription.alert.getId(), () -> EvaluationStatus.OK).get();
                EvaluationState nextState;
                if (state == null) {
                    nextState = EvaluationState.newBuilder(subscription.alert)
                            .setSince(evalTime)
                            .setLatestEval(evalTime)
                            .setStatus(status)
                            .build();
                } else {
                    nextState = state.nextStatus(status, evalTime);
                }
                Instant nextTime = getEvalTime(nextState);
                Task nextTask = new Task(subscription, nextState, nextTime);
                schedule(nextTask);
                subscription.observer.onNext(nextState);
                logger.debug("New evaluation state = {} for alert {}", nextState, subscription.alert.getId());
                getSyncEval(subscription.alert.getId()).release();
            } catch (Throwable e) {
                subscription.observer.onError(e);
                if (!(e instanceof RejectedExecutionException)) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class EvaluationSubscription implements Flow.Subscription {
        private final ProjectAssignment assignment;
        private final Alert alert;
        private final EvaluationObserver observer;
        private volatile boolean canceled;

        public EvaluationSubscription(ProjectAssignment assignment, Alert alert, EvaluationObserver observer) {
            this.assignment = assignment;
            this.alert = alert;
            this.observer = observer;
        }

        @Override
        public void request(long n) {
        }

        @Override
        public void cancel() {
            canceled = true;
        }
    }
}

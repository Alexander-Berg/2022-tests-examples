package ru.yandex.solomon.alert.rule.stubs;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.Queues;

import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.rule.AlertRule;
import ru.yandex.solomon.alert.rule.AlertRuleDeadlines;
import ru.yandex.solomon.alert.rule.SimulationResult;
import ru.yandex.solomon.alert.rule.SimulationStatus;

/**
 * @author Vladimir Gordiychuk
 */
@ParametersAreNonnullByDefault
final class ConstAlertRule implements AlertRule {
    private final Alert alert;
    private final Queue<EvaluationStatus> statuses;
    private volatile EvaluationStatus latest = EvaluationStatus.Code.NO_DATA.toStatus();

    ConstAlertRule(Alert alert, EvaluationStatus... statuses) {
        this.alert = alert;
        this.statuses = Queues.newLinkedBlockingQueue(Arrays.asList(statuses));
    }

    @Nonnull
    @Override
    public String getId() {
        return alert.getId();
    }

    @Nonnull
    @Override
    public Alert getAlert() {
        return alert;
    }

    @Override
    public CompletableFuture<EvaluationStatus> eval(Instant now, AlertRuleDeadlines deadlines) {
        EvaluationStatus status = statuses.poll();
        if (status == null) {
            return CompletableFuture.completedFuture(latest);
        }

        latest = status;
        return CompletableFuture.completedFuture(status);
    }

    @Override
    public CompletableFuture<SimulationResult> simulate(Instant from, Instant to, Duration gridStep, AlertRuleDeadlines deadlines) {
        return CompletableFuture.completedFuture(SimulationStatus.UNSUPPORTED.withMessage("Not implemented"));
    }
}

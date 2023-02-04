package ru.yandex.solomon.alert.rule.stubs;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.rule.AlertRule;
import ru.yandex.solomon.alert.rule.AlertRuleDeadlines;
import ru.yandex.solomon.alert.rule.SimulationResult;

/**
 * @author Vladimir Gordiychuk
 */
@ParametersAreNonnullByDefault
class NeverCompletedAterRule implements AlertRule {
    private final Alert alert;

    NeverCompletedAterRule(Alert alert) {
        this.alert = alert;
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
        return new CompletableFuture<>();
    }

    @Override
    public CompletableFuture<SimulationResult> simulate(Instant from, Instant to, Duration gridStep, AlertRuleDeadlines deadlines) {
        return new CompletableFuture<>();
    }
}

package ru.yandex.solomon.alert.rule.stubs;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

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
final class CountringAlertRule implements AlertRule {
    private final AlertRule delegate;
    private final AtomicInteger counter;

    CountringAlertRule(AlertRule delegate, AtomicInteger counter) {
        this.delegate = delegate;
        this.counter = counter;
    }

    @Nonnull
    @Override
    public String getId() {
        return delegate.getId();
    }

    @Nonnull
    @Override
    public Alert getAlert() {
        return delegate.getAlert();
    }

    @Override
    public CompletableFuture<EvaluationStatus> eval(Instant now, AlertRuleDeadlines deadlines) {
        counter.incrementAndGet();
        return delegate.eval(now, deadlines);
    }

    @Override
    public CompletableFuture<SimulationResult> simulate(Instant from, Instant to, Duration gridStep, AlertRuleDeadlines deadlines) {
        return delegate.simulate(from, to, gridStep, deadlines);
    }
}

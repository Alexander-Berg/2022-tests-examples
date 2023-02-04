package ru.yandex.solomon.alert.rule.stubs;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

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
class WhenReadyRule implements AlertRule {
    private final AlertRule delegate;
    private final CountDownLatch latch;

    WhenReadyRule(AlertRule delegate, CountDownLatch latch) {
        this.delegate = delegate;
        this.latch = latch;
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
        return delegate.eval(now, deadlines)
                .whenComplete((status, throwable) -> latch.countDown());
    }

    @Override
    public CompletableFuture<SimulationResult> simulate(Instant from, Instant to, Duration gridStep, AlertRuleDeadlines deadlines) {
        return delegate.simulate(from, to, gridStep, deadlines)
            .whenComplete((status, throwable) -> latch.countDown());
    }
}

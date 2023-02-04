package ru.yandex.solomon.alert.rule.stubs;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

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
class RandomDelayAlertRule implements AlertRule {
    private final AlertRule delegate;
    private final ScheduledExecutorService executorService;
    private final long maxDelayMillis;

    RandomDelayAlertRule(AlertRule delegate, ScheduledExecutorService executorService, long maxDelayMillis) {
        this.delegate = delegate;
        this.executorService = executorService;
        this.maxDelayMillis = maxDelayMillis;
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
        CompletableFuture<EvaluationStatus> future = new CompletableFuture<>();

        long randomDelayMillis = ThreadLocalRandom.current().nextLong(1, maxDelayMillis);
        executorService.schedule(() -> {
                    delegate.eval(now, deadlines)
                            .whenComplete((status, throwable) -> {
                                if (throwable != null) {
                                    future.completeExceptionally(throwable);
                                } else {
                                    future.complete(status);
                                }
                            });
                },
                randomDelayMillis, TimeUnit.MILLISECONDS);

        return future;
    }

    @Override
    public CompletableFuture<SimulationResult> simulate(Instant from, Instant to, Duration gridStep, AlertRuleDeadlines deadlines) {
        CompletableFuture<SimulationResult> future = new CompletableFuture<>();

        long randomDelayMillis = ThreadLocalRandom.current().nextLong(1, maxDelayMillis);
        executorService.schedule(() -> {
                delegate.simulate(from, to, gridStep, deadlines)
                    .whenComplete((status, throwable) -> {
                        if (throwable != null) {
                            future.completeExceptionally(throwable);
                        } else {
                            future.complete(status);
                        }
                    });
            },
            randomDelayMillis, TimeUnit.MILLISECONDS);

        return future;
    }
}

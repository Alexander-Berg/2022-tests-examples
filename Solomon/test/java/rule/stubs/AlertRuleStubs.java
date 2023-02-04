package ru.yandex.solomon.alert.rule.stubs;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.rule.AlertRule;

/**
 * @author Vladimir Gordiychuk
 */
@ParametersAreNonnullByDefault
public final class AlertRuleStubs {
    private AlertRuleStubs() {
    }

    public static AlertRule constantRule(Alert alert, EvaluationStatus.Code... codes) {
        EvaluationStatus[] statuses = Stream.of(codes)
                .map(EvaluationStatus.Code::toStatus)
                .toArray(EvaluationStatus[]::new);
        return constantRule(alert, statuses);
    }

    public static AlertRule constantRule(Alert alert, EvaluationStatus... statuses) {
        return new ConstAlertRule(alert, statuses);
    }

    public static AlertRule neverCompletedRule(Alert alert) {
        return new NeverCompletedAterRule(alert);
    }

    public static AlertRule countingRule(AlertRule rule, AtomicInteger counter) {
        return new CountringAlertRule(rule, counter);
    }

    public static AlertRule countDownWhenReadyRule(AlertRule rule, CountDownLatch latch) {
        return new WhenReadyRule(rule, latch);
    }

    public static AlertRule randomDelayRule(AlertRule rule, ScheduledExecutorService executorService) {
        return randomDelayRule(rule, executorService, TimeUnit.SECONDS.toMillis(30));
    }

    public static AlertRule randomDelayRule(AlertRule rule, ScheduledExecutorService executorService, long maxDelayMillis) {
        return new RandomDelayAlertRule(rule, executorService, maxDelayMillis);
    }
}

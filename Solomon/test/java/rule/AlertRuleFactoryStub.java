package ru.yandex.solomon.alert.rule;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.WillNotClose;

import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.domain.Alert;
import ru.yandex.solomon.alert.rule.stubs.AlertRuleStubs;

import static ru.yandex.solomon.alert.rule.stubs.AlertRuleStubs.constantRule;

/**
 * @author Vladimir Gordiychuk
 */
@ParametersAreNonnullByDefault
public class AlertRuleFactoryStub implements AlertRuleFactory {
    @WillNotClose
    private final ScheduledExecutorService executorService;
    private final ConcurrentMap<String, AlertRule> alertIdToRule = new ConcurrentHashMap<>();

    public AlertRuleFactoryStub(@WillNotClose ScheduledExecutorService executorService) {
        this.executorService = executorService;
    }

    public void setAlertRule(AlertRule rule) {
        alertIdToRule.put(rule.getId(), rule);
    }

    @Nonnull
    @Override
    public AlertRule createAlertRule(Alert alert) {
        if (!alertIdToRule.containsKey(alert.getId())) {
            AlertRule rule = AlertRuleStubs.randomDelayRule(
                    constantRule(alert, EvaluationStatus.NO_DATA),
                    executorService,
                    30
            );
            alertIdToRule.putIfAbsent(alert.getId(), rule);
        }

        return alertIdToRule.get(alert.getId());
    }
}

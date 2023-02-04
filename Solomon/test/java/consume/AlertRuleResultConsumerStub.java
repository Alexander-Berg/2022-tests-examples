package ru.yandex.solomon.alert.consume;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.solomon.alert.EvaluationStatus;
import ru.yandex.solomon.alert.evaluation.EvaluationService;
import ru.yandex.solomon.alert.rule.EvaluationState;

/**
 * @author Vladimir Gordiychuk
 */
@ParametersAreNonnullByDefault
public class AlertRuleResultConsumerStub implements EvaluationService.Consumer {
    private ConcurrentMap<String, Deque<EvaluationState>> alertIdToResultQueue = new ConcurrentHashMap<>();

    @Nullable
    public EvaluationStatus getLatestStatus(String alertId) {
        return Optional.ofNullable(alertIdToResultQueue.get(alertId))
                .map(Deque::getLast)
                .map(EvaluationState::getStatus)
                .orElse(null);
    }

    @Nullable
    public EvaluationState getLatestState(String alertId) {
        return Optional.ofNullable(alertIdToResultQueue.get(alertId))
                .map(Deque::getLast)
                .orElse(null);
    }

    @Nonnull
    public List<EvaluationState> getLatestResults(String alertId) {
        Deque<EvaluationState> queue = alertIdToResultQueue.get(alertId);

        if (queue == null) {
            return Collections.emptyList();
        }

        return new ArrayList<>(queue);
    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @Override
    public void consume(@Nonnull EvaluationState state) {
        Deque<EvaluationState> queue = alertIdToResultQueue.computeIfAbsent(state.getAlertId(),
                (ignore) -> new ConcurrentLinkedDeque<>());
        queue.add(state);
    }

    @Override
    public void onComplete() {
    }
}

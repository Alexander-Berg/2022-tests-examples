package ru.yandex.solomon.alert.cluster.broker.mute;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import javax.annotation.ParametersAreNonnullByDefault;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.solomon.alert.mute.domain.AffectingMute;
import ru.yandex.solomon.alert.mute.domain.Mute;
import ru.yandex.solomon.alert.mute.domain.MuteStatus;

/**
 * @author Ivan Tsybulin
 */
@ParametersAreNonnullByDefault
public class MuteMatcherStub implements MuteMatcher {

    private final ConcurrentMap<String, Mute> mutesById = new ConcurrentHashMap<>();

    @Override
    public List<AffectingMute> match(String alertId, Labels subAlertLabels, Instant evaluatedAt) {
        return mutesById.entrySet().stream()
                .filter(e -> e.getValue().getStatusAt(evaluatedAt) != MuteStatus.ARCHIVED && e.getValue().matches(alertId, subAlertLabels))
                .map(e -> new AffectingMute(e.getKey(), e.getValue().getStatusAt(evaluatedAt)))
                .collect(Collectors.toUnmodifiableList());
    }

    public void upsert(String id, Mute mute) {
        mutesById.put(id, mute);
    }

    public void delete(String id) {
        mutesById.remove(id);
    }
}

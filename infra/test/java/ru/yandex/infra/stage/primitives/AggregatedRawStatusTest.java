package ru.yandex.infra.stage.primitives;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import ru.yandex.infra.stage.deployunit.Readiness;
import ru.yandex.infra.stage.dto.DeployProgress;

import static java.util.Optional.empty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static ru.yandex.infra.stage.util.CustomMatchers.isReady;

public class AggregatedRawStatusTest {
    private static final DeployPrimitiveStatus STATUS_READY1 =
            new DeployPrimitiveStatus(Readiness.ready(), new DeployProgress(3, 0, 3), empty());
    private static final DeployPrimitiveStatus STATUS_READY2 =
            new DeployPrimitiveStatus(Readiness.ready(), new DeployProgress(1, 0, 1), empty());
    private static final DeployPrimitiveStatus STATUS_NOT_READY =
            new DeployPrimitiveStatus(Readiness.inProgress(""), new DeployProgress(3, 1, 4), empty());

    @Test
    void mergeTest() {
        AggregatedRawStatus result1 = AggregatedRawStatus.merge(Map.of("1", STATUS_READY1, "2", STATUS_READY2, "3", STATUS_NOT_READY));
        assertThat(result1.getReadiness(), not(isReady()));
        assertThat(result1.getProgress(), equalTo(new DeployProgress(7, 1, 8)));

        AggregatedRawStatus result2 = AggregatedRawStatus.merge(Map.of("1", STATUS_READY1, "4", STATUS_READY2));
        assertThat(result2.getReadiness(), isReady());

        var clusterStatuses = Map.of("1", STATUS_READY1);
        AggregatedRawStatus result3 = AggregatedRawStatus.merge(Map.of("1", STATUS_READY1));
        assertThat(result3, equalTo(new AggregatedRawStatus(STATUS_READY1, clusterStatuses)));
    }

    @Test
    void mergeEmptyMap() {
        AggregatedRawStatus status = AggregatedRawStatus.merge(Collections.emptyMap());
        assertThat(status, equalTo(new AggregatedRawStatus(new DeployPrimitiveStatus(Readiness.ready(), DeployProgress.EMPTY, empty()))));
    }
}

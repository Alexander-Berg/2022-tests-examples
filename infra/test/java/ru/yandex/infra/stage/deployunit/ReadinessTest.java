package ru.yandex.infra.stage.deployunit;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.infra.stage.deployunit.ReadinessDetailsTest.CLUSTER1;
import static ru.yandex.infra.stage.deployunit.ReadinessDetailsTest.CLUSTER2;
import static ru.yandex.infra.stage.deployunit.ReadinessDetailsTest.CLUSTER2_ERROR;
import static ru.yandex.infra.stage.deployunit.ReadinessDetailsTest.REASON;

// Uses some variables from ReadinessDetailsTest
class ReadinessTest {

    @Test
    void mergeAllReady() {
        Readiness merged = Readiness.mergeRelated(ImmutableMap.of(
                CLUSTER1, Readiness.ready(),
                CLUSTER2, Readiness.ready()
        ));
        assertThat(merged, equalTo(Readiness.ready()));
    }

    @Test
    void mergedIsUnreadyIfSomeAreUnready() {
        Readiness unready = Readiness.failed(REASON, CLUSTER2_ERROR);
        Readiness merged = Readiness.mergeRelated(ImmutableMap.of(
                CLUSTER1, Readiness.ready(),
                CLUSTER2, unready
        ));
        String expectedMessage = String.format("Cluster '%s': %s", CLUSTER2, CLUSTER2_ERROR);
        assertThat(merged, equalTo(Readiness.failed(REASON, expectedMessage)));
    }

    @Test
    void mergeUnrelatedAllReady() {
        Readiness merged = Readiness.mergeUnrelated(Readiness.ready(), Readiness.ready());
        assertThat(merged, equalTo(Readiness.ready()));
    }

    @Test
    void mergeUnrelatedFirstUnready() {
        Readiness unready = Readiness.inProgress(REASON);
        Readiness merged = Readiness.mergeUnrelated(unready, Readiness.ready());
        assertThat(merged, equalTo(unready));
    }

    @Test
    void mergeUnrelatedSecondUnready() {
        Readiness unready = Readiness.inProgress(REASON);
        Readiness merged = Readiness.mergeUnrelated(Readiness.ready(), unready);
        assertThat(merged, equalTo(unready));
    }

    @Test
    void mergeUnrelatedPreferFirstUnready() {
        Readiness unready1 = Readiness.inProgress("REASON1");
        Readiness unready2 = Readiness.inProgress("REASON2");
        Readiness merged = Readiness.mergeUnrelated(unready1, unready2);
        assertThat(merged, equalTo(unready1));
    }

    @Test
    void mergeRelatedOneInProgressOneFailed() {
        Readiness inProgress = Readiness.inProgress("REASON1");
        Readiness failed = Readiness.failed("REASON2", "just message");
        Readiness merged = Readiness.mergeRelated(ImmutableMap.of(
                CLUSTER1, inProgress,
                CLUSTER2, failed
        ));
        String expectedMessage = String.format("Cluster '%s': %s", CLUSTER2, "just message");
        assertThat(merged, equalTo(Readiness.failed(new ReadinessDetails("MULTIPLE_REASONS", expectedMessage),
                new ReadinessDetails("REASON2", expectedMessage))));
    }

    @Test
    void mergeRelatedOneReadyOneFailed() {
        Readiness ready = Readiness.ready();
        Readiness failed = Readiness.failed("REASON2", "just message");
        Readiness merged = Readiness.mergeRelated(ImmutableMap.of(
                CLUSTER1, ready,
                CLUSTER2, failed
        ));
        String expectedMessage = String.format("Cluster '%s': %s", CLUSTER2, "just message");
        assertThat(merged, equalTo(Readiness.failed("REASON2", expectedMessage)));
    }
}

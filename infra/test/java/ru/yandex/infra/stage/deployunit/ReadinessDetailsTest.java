package ru.yandex.infra.stage.deployunit;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class ReadinessDetailsTest {
    static final String CLUSTER1 = "cluster1";
    static final String CLUSTER2 = "cluster2";
    static final String CLUSTER1_ERROR = "cluster1 error";
    static final String CLUSTER2_ERROR = "cluster2 error";
    static final String REASON = "COMMON_REASON";
    private static final String MERGED_ERROR = String.format("Cluster '%s': %s; Cluster '%s': %s",
            CLUSTER1, CLUSTER1_ERROR, CLUSTER2, CLUSTER2_ERROR);

    @Test
    void mergeRelatedWithSameReason() {
        ReadinessDetails merged = ReadinessDetails.merge(ImmutableMap.of(
                CLUSTER1, new ReadinessDetails(REASON, CLUSTER1_ERROR),
                CLUSTER2, new ReadinessDetails(REASON, CLUSTER2_ERROR)
        ));
        assertThat(merged, equalTo(new ReadinessDetails(REASON, MERGED_ERROR)));
    }

    @Test
    void mergeRelatedWithDifferentReasons() {
        ReadinessDetails merged = ReadinessDetails.merge(ImmutableMap.of(
                CLUSTER1, new ReadinessDetails("REASON1", CLUSTER1_ERROR),
                CLUSTER2, new ReadinessDetails("REASON2", CLUSTER2_ERROR)
        ));
        assertThat(merged, equalTo(new ReadinessDetails("MULTIPLE_REASONS", MERGED_ERROR)));
    }

    @Test
    void skipEmptyMessages() {
        ReadinessDetails merged = ReadinessDetails.merge(ImmutableMap.of(
                CLUSTER1, new ReadinessDetails("REASON1", ""),
                CLUSTER2, new ReadinessDetails("REASON2", CLUSTER2_ERROR)
        ));
        assertThat(merged, equalTo(new ReadinessDetails("MULTIPLE_REASONS",
                String.format("Cluster '%s': %s", CLUSTER2, CLUSTER2_ERROR))));
    }

    @Test
    void mergeRelatedWithEmptyMessages() {
        ReadinessDetails merged = ReadinessDetails.merge(ImmutableMap.of(
                CLUSTER1, new ReadinessDetails("REASON1"),
                CLUSTER2, new ReadinessDetails("REASON2")
        ));
        assertThat(merged, equalTo(new ReadinessDetails("MULTIPLE_REASONS", "")));

    }
}

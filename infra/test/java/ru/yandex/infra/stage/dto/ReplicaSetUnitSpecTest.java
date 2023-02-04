package ru.yandex.infra.stage.dto;

import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import ru.yandex.bolts.collection.Either;
import ru.yandex.infra.stage.yp.Retainment;
import ru.yandex.yp.client.api.TReplicaSetScaleSpec;
import ru.yandex.yp.client.api.TReplicaSetSpec;
import ru.yandex.yp.model.YpObjectType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.infra.stage.TestData.POD_AGENT_CONFIG_EXTRACTOR;
import static ru.yandex.yp.model.YpObjectType.HORIZONTAL_POD_AUTOSCALER;

class ReplicaSetUnitSpecTest extends UnitSpecDetailsTestBase {
    private static final String CLUSTER_IN_SPEC = "sas";
    private static final String CLUSTER_NOT_IN_SPEC = "vla";
    private static final ReplicaSetUnitSpec SPEC = new ReplicaSetUnitSpec(TReplicaSetSpec.getDefaultInstance(),
            ImmutableMap.of(
                    CLUSTER_IN_SPEC, new ReplicaSetUnitSpec.PerClusterSettings(Either.left(10), Optional.empty())
            ),
            POD_AGENT_CONFIG_EXTRACTOR
    );
    private static final ReplicaSetUnitSpec AUTOSCALING_SPEC = new ReplicaSetUnitSpec(TReplicaSetSpec.getDefaultInstance(),
            ImmutableMap.of(
                    CLUSTER_IN_SPEC, new ReplicaSetUnitSpec.PerClusterSettings(
                            Either.right(TReplicaSetScaleSpec.newBuilder().build()), Optional.empty())
            ),
            POD_AGENT_CONFIG_EXTRACTOR
    );

    @Override
    protected DeployUnitSpecDetails getSpec() {
        return SPEC;
    }

    @Test
    void retainObjectInCluster() {
        assertRetained(ClusterAndType.perClusterInstance(CLUSTER_IN_SPEC, YpObjectType.REPLICA_SET));
        assertRetained(ClusterAndType.perClusterInstance(CLUSTER_IN_SPEC, YpObjectType.ENDPOINT_SET));
    }

    @Test
    void retainUnknownObject() {
        assertRetained(ClusterAndType.perClusterInstance(CLUSTER_NOT_IN_SPEC, YpObjectType.STAGE));
    }

    @Test
    void retainHorizontalPodAutoscalerWithEnabledAutoscale() {
        assertRetained(AUTOSCALING_SPEC, ClusterAndType.perClusterInstance(CLUSTER_IN_SPEC, HORIZONTAL_POD_AUTOSCALER));
    }

    @Test
    void notRetainHorizontalPodAutoscalerWithDisabledAutoscale() {
        assertThat(SPEC.shouldRetain(ClusterAndType.perClusterInstance(CLUSTER_IN_SPEC, HORIZONTAL_POD_AUTOSCALER)),
                equalTo(new Retainment(false, String.format("Replica set does not have %s in cluster '%s'",
                        HORIZONTAL_POD_AUTOSCALER, CLUSTER_IN_SPEC))));
    }

    @Test
    void notRetainObjectInAbsentCluster() {
        assertThat(SPEC.shouldRetain(ClusterAndType.perClusterInstance(CLUSTER_NOT_IN_SPEC, YpObjectType.REPLICA_SET)),
                equalTo(new Retainment(false, String.format("Replica set does not have pods in cluster '%s'", CLUSTER_NOT_IN_SPEC))));
    }
}

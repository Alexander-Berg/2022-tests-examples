package ru.yandex.infra.stage.dto;

import org.junit.jupiter.api.Test;

import ru.yandex.infra.stage.yp.Retainment;
import ru.yandex.yp.client.api.TMultiClusterReplicaSetSpec;
import ru.yandex.yp.model.YpObjectType;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.infra.stage.TestData.POD_AGENT_CONFIG_EXTRACTOR;

class McrsUnitSpecTest extends UnitSpecDetailsTestBase {
    private static final String CLUSTER_IN_SPEC = "sas";
    private static final String CLUSTER_NOT_IN_SPEC = "vla";
    private static final McrsUnitSpec SPEC = new McrsUnitSpec(TMultiClusterReplicaSetSpec.newBuilder()
            .addClusters(TMultiClusterReplicaSetSpec.TClusterReplicaSetSpecPreferences.newBuilder()
                    .setCluster(CLUSTER_IN_SPEC)
                    .build())
            .build(),
            POD_AGENT_CONFIG_EXTRACTOR
    );

    @Override
    protected DeployUnitSpecDetails getSpec() {
        return SPEC;
    }

    @Test
    void retainEndpointSetInCluster() {
        assertRetained(ClusterAndType.perClusterInstance(CLUSTER_IN_SPEC, YpObjectType.ENDPOINT_SET));
    }

    @Test
    void retainMcrs() {
        assertRetained(ClusterAndType.mcrs());
    }

    @Test
    void retainUnknownObject() {
        assertRetained(ClusterAndType.perClusterInstance(CLUSTER_NOT_IN_SPEC, YpObjectType.STAGE));
    }

    @Test
    void notRetainObjectInAbsentCluster() {
        assertThat(SPEC.shouldRetain(ClusterAndType.perClusterInstance(CLUSTER_NOT_IN_SPEC, YpObjectType.ENDPOINT_SET)),
                equalTo(new Retainment(false, String.format("MCRS does not have pods in cluster '%s'", CLUSTER_NOT_IN_SPEC))));
    }
}

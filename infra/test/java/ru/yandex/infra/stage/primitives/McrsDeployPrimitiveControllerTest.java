package ru.yandex.infra.stage.primitives;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.deployunit.DummyObjectController;
import ru.yandex.infra.stage.deployunit.Readiness;
import ru.yandex.infra.stage.dto.DeployProgress;
import ru.yandex.infra.stage.dto.McrsUnitSpec;
import ru.yandex.infra.stage.dto.datamodel.AntiaffinityConstraint;
import ru.yandex.infra.stage.podspecs.DummyNullPatcher;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.yp.client.api.DataModel;
import ru.yandex.yp.client.api.TMultiClusterReplicaSetSpec;
import ru.yandex.yp.client.api.TMultiClusterReplicaSetStatus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static ru.yandex.infra.stage.TestData.DEFAULT_UNIT_CONTEXT;
import static ru.yandex.infra.stage.TestData.POD_AGENT_CONFIG_EXTRACTOR;
import static ru.yandex.infra.stage.primitives.DeployPrimitiveController.DEPLOY_LABEL_KEY;
import static ru.yandex.infra.stage.primitives.DeployPrimitiveController.STAGE_LABEL_KEY;

class McrsDeployPrimitiveControllerTest {
    private static final String CLUSTER1 = "cluster1";
    private static final String CLUSTER2 = "cluster2";

    @Test
    void setDefaultAntiaffinityIfAllUnset() {
        TMultiClusterReplicaSetSpec spec = TMultiClusterReplicaSetSpec.newBuilder()
                .addClusters(emptyPreferences(CLUSTER1))
                .addClusters(emptyPreferences(CLUSTER2))
                .build();
        constraintsTestTemplate(spec, ImmutableMap.of(
                CLUSTER1, ImmutableList.of(TestData.CONVERTER.toProto(AntiaffinityConstraint.node(1))),
                CLUSTER2, ImmutableList.of(TestData.CONVERTER.toProto(AntiaffinityConstraint.node(1)))
        ));
    }

    @Test
    void notReplaceExistingAntiaffinityConstraints() {
        DataModel.TAntiaffinityConstraint existing = TestData.CONVERTER.toProto(TestData.CONSTRAINT);
        TMultiClusterReplicaSetSpec spec = TMultiClusterReplicaSetSpec.newBuilder()
                .addClusters(TMultiClusterReplicaSetSpec.TClusterReplicaSetSpecPreferences.newBuilder()
                        .setCluster(CLUSTER1)
                        .setSpec(TMultiClusterReplicaSetSpec.TReplicaSetSpecPreferences.newBuilder()
                                .setConstraints(TMultiClusterReplicaSetSpec.TConstraints.newBuilder()
                                        .addAntiaffinityConstraints(existing)
                                        .build())
                                .build())
                        .build())
                .addClusters(emptyPreferences(CLUSTER2))
                .build();
        constraintsTestTemplate(spec, ImmutableMap.of(
                CLUSTER1, ImmutableList.of(existing),
                CLUSTER2, Collections.emptyList()
        ));
    }

    @Test
    void stageLabelTest() {
        DummyObjectController<TMultiClusterReplicaSetSpec, DeployPrimitiveStatus<TMultiClusterReplicaSetStatus>> customReplicaSetController =
                new DummyObjectController<>();
        customReplicaSetController.setStatus(new DeployPrimitiveStatus<>(Readiness.ready(), DeployProgress.EMPTY, Optional.empty()));

        McrsDeployPrimitiveController controller =
                new McrsDeployPrimitiveController(
                        customReplicaSetController,
                        DummyNullPatcher.INSTANCE,
                        TestData.CONVERTER);

        McrsUnitSpec unitSpec = new McrsUnitSpec(TMultiClusterReplicaSetSpec.getDefaultInstance(), POD_AGENT_CONFIG_EXTRACTOR);
        controller.sync(unitSpec, DEFAULT_UNIT_CONTEXT);

        assertThat(customReplicaSetController.getCurrentLabels(), hasKey(DEPLOY_LABEL_KEY));
        assertThat(customReplicaSetController.getCurrentLabels().get(DEPLOY_LABEL_KEY).asMap(),
                hasEntry(STAGE_LABEL_KEY, YTree.stringNode(DEFAULT_UNIT_CONTEXT.getStageContext().getStageId())));
    }

    @Test
    void notSetLabelsByDefault() {
        TMultiClusterReplicaSetSpec spec = TMultiClusterReplicaSetSpec.newBuilder()
                .addClusters(emptyPreferences(CLUSTER1))
                .build();
        TMultiClusterReplicaSetSpec mergedSpec = doMergeSpec(spec);
        assertThat("labels should not be set", !mergedSpec.getPodTemplateSpec().hasLabels());
    }

    private static TMultiClusterReplicaSetSpec doMergeSpec(TMultiClusterReplicaSetSpec incomingSpec) {
        McrsUnitSpec unitSpec = new McrsUnitSpec(incomingSpec, POD_AGENT_CONFIG_EXTRACTOR);
        return McrsDeployPrimitiveController.mergeSpec(unitSpec, TestData.DEFAULT_UNIT_CONTEXT, DummyNullPatcher.INSTANCE,
                TestData.CONVERTER);
    }

    private static void constraintsTestTemplate(TMultiClusterReplicaSetSpec incomingSpec,
                                                Map<String, List<DataModel.TAntiaffinityConstraint>> outgoingConstraints) {
        TMultiClusterReplicaSetSpec mergedSpec = doMergeSpec(incomingSpec);
        Map<String, List<DataModel.TAntiaffinityConstraint>> actualConstraints = mergedSpec.getClustersList().stream()
                .collect(Collectors.toMap(TMultiClusterReplicaSetSpec.TClusterReplicaSetSpecPreferences::getCluster,
                        entry -> entry.getSpec().getConstraints().getAntiaffinityConstraintsList()));

        assertThat(actualConstraints, equalTo(outgoingConstraints));
    }

    private static TMultiClusterReplicaSetSpec.TClusterReplicaSetSpecPreferences emptyPreferences(String cluster) {
        return TMultiClusterReplicaSetSpec.TClusterReplicaSetSpecPreferences.newBuilder()
                .setCluster(cluster)
                .build();
    }
}

package ru.yandex.infra.stage.primitives;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;

import ru.yandex.bolts.collection.Either;
import ru.yandex.infra.stage.StageContext;
import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.deployunit.DeployUnitContext;
import ru.yandex.infra.stage.deployunit.DummyObjectController;
import ru.yandex.infra.stage.deployunit.MultiplexingController;
import ru.yandex.infra.stage.deployunit.Readiness;
import ru.yandex.infra.stage.dto.DeployProgress;
import ru.yandex.infra.stage.dto.DeployReadyCriterion;
import ru.yandex.infra.stage.dto.DeploySpeed;
import ru.yandex.infra.stage.dto.DeployUnitSpec;
import ru.yandex.infra.stage.dto.ReplicaSetDeploymentStrategy;
import ru.yandex.infra.stage.dto.ReplicaSetUnitSpec;
import ru.yandex.infra.stage.dto.RuntimeDeployControls;
import ru.yandex.infra.stage.dto.datamodel.AntiaffinityConstraint;
import ru.yandex.infra.stage.podspecs.DummyNullPatcher;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeListNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.inside.yt.kosher.ytree.YTreeListNode;
import ru.yandex.yp.client.api.DataModel;
import ru.yandex.yp.client.api.TDeployUnitSpec;
import ru.yandex.yp.client.api.TReplicaSetScaleSpec;
import ru.yandex.yp.client.api.TReplicaSetSpec;
import ru.yandex.yp.client.api.TReplicaSetStatus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static ru.yandex.infra.stage.TestData.DEFAULT_UNIT_CONTEXT;
import static ru.yandex.infra.stage.TestData.POD_AGENT_CONFIG_EXTRACTOR;
import static ru.yandex.infra.stage.primitives.DeployPrimitiveController.DEPLOY_LABEL_KEY;
import static ru.yandex.infra.stage.primitives.DeployPrimitiveController.STAGE_LABEL_KEY;
import static ru.yandex.infra.stage.primitives.ReplicaSetDeployPrimitiveController.AUTOSCALER_ID_LABEL_KEY;
import static ru.yandex.infra.stage.util.GeneralUtils.CLUSTER_SEQUENCE;

class ReplicaSetDeployPrimitiveControllerTest {

    public static final String ANOTHER_CLUSTER = "another-cluster";

    @Test
    void setDefaultAntiaffinityIfUnset() {
        constraintsTestTemplate(TReplicaSetSpec.getDefaultInstance(),
                ImmutableList.of(TestData.CONVERTER.toProto(AntiaffinityConstraint.node(1))));
    }

    @Test
    void notReplaceExistingAntiaffinityConstraints() {
        DataModel.TAntiaffinityConstraint constraint = TestData.CONVERTER.toProto(TestData.CONSTRAINT);
        constraintsTestTemplate(TReplicaSetSpec.newBuilder()
                .setConstraints(TReplicaSetSpec.TConstraints.newBuilder()
                        .addAntiaffinityConstraints(constraint)
                        .build())
                .build(), ImmutableList.of(constraint));
    }

    @Test
    void notSetLabelsByDefault() {
        TReplicaSetSpec mergedSpec = doMergeSpec(TReplicaSetSpec.getDefaultInstance());
        assertThat("labels should not be set", !mergedSpec.getPodTemplateSpec().hasLabels());
    }

    @Test
    void patchDeploymentStrategyWithDeductedRevisionIdTest() {
        ImmutableMap.of(1234, "1234",
                Integer.MIN_VALUE, "2147483648",
                Integer.MAX_VALUE, "2147483647",
                Integer.MAX_VALUE + 2, "2147483649")
                .forEach((intFromProto, stringRepresentation) -> {
                    TReplicaSetSpec newSpec = patchDeploymentStrategy(
                            DEFAULT_UNIT_CONTEXT.withDeployUnitSpec(
                                    DeployUnitSpec::withRevision, intFromProto
                            )
                    );
                    assertThat(newSpec.getRevisionId(), equalTo(stringRepresentation));
                });
    }

    @Test
    void patchDeploymentStrategyTest() {
        patchDeploymentStrategy(DEFAULT_UNIT_CONTEXT);
    }

    private TReplicaSetSpec patchDeploymentStrategy(DeployUnitContext unitContext) {
        TReplicaSetSpec.TDeploymentStrategy strategy = TReplicaSetSpec.TDeploymentStrategy.newBuilder()
                .setMaxUnavailable(1)
                .build();
        ReplicaSetDeploymentStrategy newStrategy = new ReplicaSetDeploymentStrategy(strategy.getMinAvailable() + 1,
                                                                                    strategy.getMaxUnavailable() + 1,
                                                                                    strategy.getMaxSurge() + 1,
                                                                                    strategy.getMinCreatedSeconds() + 1,
                                                                                    strategy.getMaxTolerableDowntimeSeconds() + 1,
                                                                                    strategy.getMaxTolerableDowntimePods() + 1,
                                                                                    Optional.of(new DeploySpeed(1, 2)), Optional.of(new DeployReadyCriterion(Optional.of("AUTO"), Optional.of(1), Optional.of(1))));
        TReplicaSetSpec spec = TReplicaSetSpec.newBuilder()
                .setDeploymentStrategy(strategy)
                .build();

        ReplicaSetUnitSpec unitSpec = new ReplicaSetUnitSpec(spec, ImmutableMap.of(
                TestData.CLUSTER, new ReplicaSetUnitSpec.PerClusterSettings(Either.left(1), Optional.of(newStrategy))),
                POD_AGENT_CONFIG_EXTRACTOR);
        TReplicaSetSpec newSpec = ReplicaSetDeployPrimitiveController.mergeSpec(unitSpec, unitContext,
                TestData.CLUSTER, DummyNullPatcher.INSTANCE, TestData.CONVERTER);

        assertThat(newSpec.getDeploymentStrategy(), equalTo(TestData.CONVERTER.toProto(newStrategy)));
        return newSpec;
    }

    @Test
    void perClusterDeployTest() {
        String replicaSetId = "replica_set_id";
        MultiplexingController<TReplicaSetSpec, DeployPrimitiveStatus<TReplicaSetStatus>> multiplexingController =
                mock(MultiplexingController.class);
        List<InvocationOnMock> invocations = new ArrayList<>();
        doAnswer(args -> {
            invocations.add(args);
            return null;
        }).when(multiplexingController)
                .syncSequential(anyMap(), any(), anySet(), anyMap(), anyList());


        ReplicaSetDeployPrimitiveController controller =
                new ReplicaSetDeployPrimitiveController(
                        replicaSetId,
                        multiplexingController,
                        DummyNullPatcher.INSTANCE,
                        TestData.CONVERTER);

        ReplicaSetUnitSpec unitSpec = new ReplicaSetUnitSpec(TReplicaSetSpec.newBuilder().build(), ImmutableMap.of(
                TestData.CLUSTER,
                new ReplicaSetUnitSpec.PerClusterSettings(Either.right(
                        TReplicaSetScaleSpec.newBuilder()
                                .setMinReplicas(1)
                                .setMaxReplicas(5)
                                .build()),
                        Optional.empty())),
                POD_AGENT_CONFIG_EXTRACTOR);

        DeployUnitSpec.DeploySettings.ClusterSettings clusterSettings =
                new DeployUnitSpec.DeploySettings.ClusterSettings(TestData.CLUSTER, true);
        ImmutableList<DeployUnitSpec.DeploySettings.ClusterSettings> clusters =
                ImmutableList.of(clusterSettings);
        DeployUnitSpec.DeploySettings deploySettings =
                new DeployUnitSpec.DeploySettings(clusters, TDeployUnitSpec.TDeploySettings.EDeployStrategy.SEQUENTIAL);
        controller.sync(
                unitSpec,
                DEFAULT_UNIT_CONTEXT.withDeploySettings(deploySettings)
        );
        InvocationOnMock firstInvocation = invocations.remove(0);
        assertThat(firstInvocation.getArgument(4), contains(Pair.of(clusterSettings.getName(),
                clusterSettings.isNeedApprove())));
    }

    @Test
    void changeDeployPolicyTest() {
        String replicaSetId = "replica_set_id";
        MultiplexingController<TReplicaSetSpec, DeployPrimitiveStatus<TReplicaSetStatus>> multiplexingController =
                mock(MultiplexingController.class);
        List<InvocationOnMock> invocations = new ArrayList<>();
        doAnswer(args -> {
            invocations.add(args);
            return null;
        }).when(multiplexingController)
                .syncSequential(anyMap(), any(), anySet(), anyMap(), anyList());


        ReplicaSetDeployPrimitiveController controller =
                new ReplicaSetDeployPrimitiveController(
                        replicaSetId,
                        multiplexingController,
                        DummyNullPatcher.INSTANCE,
                        TestData.CONVERTER);

        ReplicaSetUnitSpec unitSpec = new ReplicaSetUnitSpec(TReplicaSetSpec.newBuilder().build(), ImmutableMap.of(
                TestData.CLUSTER,
                new ReplicaSetUnitSpec.PerClusterSettings(Either.right(
                        TReplicaSetScaleSpec.newBuilder()
                                .setMinReplicas(1)
                                .setMaxReplicas(5)
                                .build()),
                        Optional.empty())),
                POD_AGENT_CONFIG_EXTRACTOR);

        DeployUnitSpec.DeploySettings.ClusterSettings clusterSettings =
                new DeployUnitSpec.DeploySettings.ClusterSettings(TestData.CLUSTER, true);
        ImmutableList<DeployUnitSpec.DeploySettings.ClusterSettings> clusters =
                ImmutableList.of(clusterSettings);
        DeployUnitSpec.DeploySettings deploySettings =
                new DeployUnitSpec.DeploySettings(clusters, TDeployUnitSpec.TDeploySettings.EDeployStrategy.SEQUENTIAL);
        RuntimeDeployControls runtimeDeployControls = new RuntimeDeployControls(Collections.emptyMap(),
                Collections.emptyMap(), Collections.singletonMap(DEFAULT_UNIT_CONTEXT.getDeployUnitId(),
                deploySettings));
        controller.sync(
                unitSpec,
                DEFAULT_UNIT_CONTEXT.withStageContext(
                        StageContext::withRuntimeDeployControls,
                        runtimeDeployControls
                )
        );
        InvocationOnMock firstInvocation = invocations.remove(0);
        assertThat(firstInvocation.getArgument(4), contains(Pair.of(clusterSettings.getName(),
                clusterSettings.isNeedApprove())));
    }

    @Test
    void perClusterDeployFullSetTest() {
        String replicaSetId = "replica_set_id";
        MultiplexingController<TReplicaSetSpec, DeployPrimitiveStatus<TReplicaSetStatus>> multiplexingController =
                mock(MultiplexingController.class);
        List<InvocationOnMock> invocations = new ArrayList<>();
        doAnswer(args -> {
            invocations.add(args);
            return null;
        }).when(multiplexingController)
                .syncSequential(anyMap(), any(), anySet(), anyMap(), anyList());


        ReplicaSetDeployPrimitiveController controller =
                new ReplicaSetDeployPrimitiveController(
                        replicaSetId,
                        multiplexingController,
                        DummyNullPatcher.INSTANCE,
                        TestData.CONVERTER);

        ReplicaSetUnitSpec unitSpec = new ReplicaSetUnitSpec(TReplicaSetSpec.newBuilder().build(), ImmutableMap.of(
                TestData.CLUSTER,
                new ReplicaSetUnitSpec.PerClusterSettings(Either.right(
                        TReplicaSetScaleSpec.newBuilder()
                                .setMinReplicas(1)
                                .setMaxReplicas(5)
                                .build()),
                        Optional.empty()),
                ANOTHER_CLUSTER,
                new ReplicaSetUnitSpec.PerClusterSettings(Either.right(
                        TReplicaSetScaleSpec.newBuilder()
                                .setMinReplicas(1)
                                .setMaxReplicas(5)
                                .build()),
                        Optional.empty())),
                POD_AGENT_CONFIG_EXTRACTOR);

        DeployUnitSpec.DeploySettings.ClusterSettings clusterSettings =
                new DeployUnitSpec.DeploySettings.ClusterSettings(TestData.CLUSTER, true);
        ImmutableList<DeployUnitSpec.DeploySettings.ClusterSettings> clusters =
                ImmutableList.of(clusterSettings);
        DeployUnitSpec.DeploySettings deploySettings =
                new DeployUnitSpec.DeploySettings(clusters, TDeployUnitSpec.TDeploySettings.EDeployStrategy.SEQUENTIAL);
        controller.sync(
                unitSpec,
                DEFAULT_UNIT_CONTEXT.withDeploySettings(deploySettings)
        );
        InvocationOnMock firstInvocation = invocations.remove(0);
        assertThat(firstInvocation.getArgument(4), contains(Pair.of(clusterSettings.getName(),
                clusterSettings.isNeedApprove()), Pair.of(ANOTHER_CLUSTER,false)));
    }

    @Test
    void perClusterDeployTestBackwardCompatibility() {
        String replicaSetId = "replica_set_id";
        MultiplexingController<TReplicaSetSpec, DeployPrimitiveStatus<TReplicaSetStatus>> multiplexingController =
                mock(MultiplexingController.class);
        List<InvocationOnMock> invocations = new ArrayList<>();
        doAnswer(args -> {
            invocations.add(args);
            return null;
        }).when(multiplexingController)
                .syncSequential(anyMap(), any(), anySet(), anyMap(), anyList());


        ReplicaSetDeployPrimitiveController controller =
                new ReplicaSetDeployPrimitiveController(
                        replicaSetId,
                        multiplexingController,
                        DummyNullPatcher.INSTANCE,
                        TestData.CONVERTER);

        ReplicaSetUnitSpec unitSpec = new ReplicaSetUnitSpec(TReplicaSetSpec.newBuilder().build(), ImmutableMap.of(
                TestData.CLUSTER,
                new ReplicaSetUnitSpec.PerClusterSettings(Either.right(
                        TReplicaSetScaleSpec.newBuilder()
                                .setMinReplicas(1)
                                .setMaxReplicas(5)
                                .build()),
                        Optional.empty())),
                POD_AGENT_CONFIG_EXTRACTOR);

        YTreeListNode yTreeNodes = new YTreeListNodeImpl(Collections.emptyMap());
        yTreeNodes.add(new YTreeStringNodeImpl(TestData.CLUSTER, Collections.emptyMap()));

        controller.sync(
                unitSpec,
                DEFAULT_UNIT_CONTEXT
                        .withLabels(ImmutableMap.of(CLUSTER_SEQUENCE, yTreeNodes))
                        .withoutDeploySettings()
        );

        InvocationOnMock firstInvocation = invocations.remove(0);
        assertThat(firstInvocation.getArgument(4), contains(Pair.of(TestData.CLUSTER, false)));
    }

    @Test
    void addAutoscalerIdInLabelsTest() {
        String replicaSetId = "replica_set_id";
        DummyObjectController<TReplicaSetSpec, DeployPrimitiveStatus<TReplicaSetStatus>> customReplicaSetController =
                new DummyObjectController<>();
        customReplicaSetController.setStatus(new DeployPrimitiveStatus(Readiness.ready(), DeployProgress.EMPTY, Optional.empty()));

        ReplicaSetDeployPrimitiveController controller =
                new ReplicaSetDeployPrimitiveController(
                        replicaSetId,
                        (a, b, c) -> customReplicaSetController,
                        DummyNullPatcher.INSTANCE,
                        (v) -> {
                        },
                        TestData.CONVERTER);

        ReplicaSetUnitSpec unitSpec = new ReplicaSetUnitSpec(TReplicaSetSpec.newBuilder().build(), ImmutableMap.of(
                TestData.CLUSTER,
                new ReplicaSetUnitSpec.PerClusterSettings(Either.right(
                        TReplicaSetScaleSpec.newBuilder()
                                .setMinReplicas(1)
                                .setMaxReplicas(5)
                                .build()),
                        Optional.empty())),
                POD_AGENT_CONFIG_EXTRACTOR);
        controller.sync(unitSpec, DEFAULT_UNIT_CONTEXT);

        assertThat(customReplicaSetController.getCurrentLabels(), hasKey(DEPLOY_LABEL_KEY));
        assertThat(customReplicaSetController.getCurrentLabels().get(DEPLOY_LABEL_KEY).asMap(),
                hasEntry(AUTOSCALER_ID_LABEL_KEY, YTree.stringNode(replicaSetId)));
    }

    @Test
    void stageLabelTest() {
        DummyObjectController<TReplicaSetSpec, DeployPrimitiveStatus<TReplicaSetStatus>> customReplicaSetController =
                new DummyObjectController<>();

        ReplicaSetDeployPrimitiveController controller =
                new ReplicaSetDeployPrimitiveController(
                        "replica_set_id",
                        (a, b, c) -> customReplicaSetController,
                        DummyNullPatcher.INSTANCE,
                        (v) -> {},
                        TestData.CONVERTER);

        ReplicaSetUnitSpec unitSpec = new ReplicaSetUnitSpec(TReplicaSetSpec.getDefaultInstance(), ImmutableMap.of(
                TestData.CLUSTER,
                new ReplicaSetUnitSpec.PerClusterSettings(Either.left(1), Optional.empty())),
                POD_AGENT_CONFIG_EXTRACTOR);
        controller.sync(unitSpec, DEFAULT_UNIT_CONTEXT);

        assertThat(customReplicaSetController.getCurrentLabels(), hasKey(DEPLOY_LABEL_KEY));
        assertThat(customReplicaSetController.getCurrentLabels().get(DEPLOY_LABEL_KEY).asMap(),
                hasEntry(STAGE_LABEL_KEY, YTree.stringNode(DEFAULT_UNIT_CONTEXT.getStageContext().getStageId())));
    }

    private static TReplicaSetSpec doMergeSpec(TReplicaSetSpec incomingSpec) {
        ReplicaSetUnitSpec unitSpec = new ReplicaSetUnitSpec(incomingSpec, ImmutableMap.of(
                TestData.CLUSTER, new ReplicaSetUnitSpec.PerClusterSettings(Either.left(1), Optional.empty())),
                POD_AGENT_CONFIG_EXTRACTOR);
        return ReplicaSetDeployPrimitiveController.mergeSpec(unitSpec, DEFAULT_UNIT_CONTEXT,
                TestData.CLUSTER, DummyNullPatcher.INSTANCE, TestData.CONVERTER);
    }

    private static void constraintsTestTemplate(TReplicaSetSpec incomingSpec,
                                                List<DataModel.TAntiaffinityConstraint> outgoingConstraints) {
        TReplicaSetSpec mergedSpec = doMergeSpec(incomingSpec);
        assertThat(mergedSpec.getConstraints(), equalTo(TReplicaSetSpec.TConstraints.newBuilder()
                .addAllAntiaffinityConstraints(outgoingConstraints)
                .build()));
    }
}

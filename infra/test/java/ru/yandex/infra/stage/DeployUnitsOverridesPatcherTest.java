package ru.yandex.infra.stage;

import java.util.Map;
import java.util.Optional;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import ru.yandex.bolts.collection.Either;
import ru.yandex.infra.stage.dto.DeployReadyCriterion;
import ru.yandex.infra.stage.dto.DeployUnitOverrides;
import ru.yandex.infra.stage.dto.ReplicaSetDeploymentStrategy;
import ru.yandex.infra.stage.dto.ReplicaSetUnitSpec;
import ru.yandex.infra.stage.dto.StageSpec;
import ru.yandex.yp.client.api.TDeployUnitSpec;
import ru.yandex.yp.client.api.TDeployUnitSpec.TReplicaSetDeploy.TPerClusterSettings;
import ru.yandex.yp.client.api.TMultiClusterReplicaSetSpec;
import ru.yandex.yp.client.api.TReplicaSetSpec;
import ru.yandex.yp.client.api.TStageSpec;

import static java.util.Collections.emptyMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.infra.stage.TestData.ABC_ACCOUNT_ID;
import static ru.yandex.infra.stage.TestData.CONVERTER;
import static ru.yandex.infra.stage.TestData.DEPLOY_UNIT_ID;
import static ru.yandex.infra.stage.TestData.DEPLOY_UNIT_SPEC;
import static ru.yandex.infra.stage.TestData.MAX_UNAVAILABLE;
import static ru.yandex.infra.stage.TestData.MCRS_UNIT_SPEC;
import static ru.yandex.infra.stage.TestData.POD_AGENT_CONFIG_EXTRACTOR;
import static ru.yandex.infra.stage.TestData.REPLICA_SET_SPEC;

public class DeployUnitsOverridesPatcherTest {
    private static final String DU_ID1 = DEPLOY_UNIT_ID + "1";
    private static final String DU_ID2 = DEPLOY_UNIT_ID + "2";
    private static final String CLUSTER1 = "sas";
    private static final String CLUSTER2 = "man";
    private static final String MCRS_CLUSTER = "multi";
    private static final int NEW_MAX_UNAVAILABLE = 5;

    public static final ReplicaSetUnitSpec RS_UNIT_SPEC;

    static {
        DeployReadyCriterion deployReadyCriterion = new DeployReadyCriterion(Optional.of("AUTO"), Optional.of(1), Optional.of(1));
        RS_UNIT_SPEC = new ReplicaSetUnitSpec(REPLICA_SET_SPEC,
                ImmutableMap.of(
                        CLUSTER1, new ReplicaSetUnitSpec.PerClusterSettings(Either.left(100), Optional.of(
                                new ReplicaSetDeploymentStrategy(0, MAX_UNAVAILABLE, 0, 0, 0, 0, Optional.empty(), Optional.of(deployReadyCriterion)))),
                        CLUSTER2, new ReplicaSetUnitSpec.PerClusterSettings(Either.left(100), Optional.of(
                                new ReplicaSetDeploymentStrategy(0, MAX_UNAVAILABLE, 0, 0, 0, 0, Optional.empty(), Optional.of(deployReadyCriterion))))),
                POD_AGENT_CONFIG_EXTRACTOR);
    }

    public static final TStageSpec STAGE = CONVERTER.toProto(new StageSpec(
            ImmutableMap.of(
                    DU_ID1, DEPLOY_UNIT_SPEC.withDetails(RS_UNIT_SPEC),
                    DU_ID2, DEPLOY_UNIT_SPEC.withDetails(MCRS_UNIT_SPEC)),
            ABC_ACCOUNT_ID, 100500, false, emptyMap(), emptyMap()));

    private TReplicaSetSpec.TDeploymentStrategy getRsDeploymentStrategy(
            TStageSpec spec, String duId, String cluster) {
        return spec.getDeployUnitsMap().get(duId).getReplicaSet().getPerClusterSettingsMap().get(cluster)
                .getDeploymentStrategy();
    }

    private TMultiClusterReplicaSetSpec.TDeploymentStrategy getMcrsDeploymentStrategy(TStageSpec spec, String duId) {
        return spec.getDeployUnitsMap().get(duId).getMultiClusterReplicaSet().getReplicaSet().getDeploymentStrategy();
    }

    private TStageSpec patchMaxUnavailable(TStageSpec spec, String duId, Optional<String> cluster, int value) {
        TStageSpec.Builder specBuilder = spec.toBuilder();
        TDeployUnitSpec.Builder duBuilder = specBuilder.getDeployUnitsOrThrow(duId).toBuilder();

        if (cluster.isEmpty()) {
            duBuilder.getMultiClusterReplicaSetBuilder().getReplicaSetBuilder().getDeploymentStrategyBuilder()
                    .setMaxUnavailable(value);
        } else {
            TPerClusterSettings.Builder settingsBuilder =
                    duBuilder.getReplicaSetBuilder().getPerClusterSettingsOrThrow(cluster.get()).toBuilder();
            settingsBuilder.getDeploymentStrategyBuilder().setMaxUnavailable(value);
            duBuilder.getReplicaSetBuilder().putPerClusterSettings(cluster.get(), settingsBuilder.build());
        }

        specBuilder.putDeployUnits(duId, duBuilder.build());
        return specBuilder.build();
    }

    @Test
    public void testEmptyOverrides() {
        Map<String, DeployUnitOverrides> deployUnitOverrides = emptyMap();
        DeployUnitsOverridesPatcher patcher = new DeployUnitsOverridesPatcher(deployUnitOverrides);
        TStageSpec.Builder stageBuilder = STAGE.toBuilder();
        patcher.patch(stageBuilder);
        assertThat(stageBuilder.build(), equalTo(STAGE));
    }

    @Test
    public void testOverrideOneLocationInRsDeployUnit() {
        Map<String, DeployUnitOverrides> deployUnitOverrides = Map.of(DU_ID1, new DeployUnitOverrides(
                Map.of(CLUSTER1, new DeployUnitOverrides.PerClusterOverrides(NEW_MAX_UNAVAILABLE)), 1));
        DeployUnitsOverridesPatcher patcher = new DeployUnitsOverridesPatcher(deployUnitOverrides);
        TStageSpec.Builder stageBuilder = STAGE.toBuilder();
        patcher.patch(stageBuilder);
        TStageSpec patchedSpec = stageBuilder.build();

        assertThat(getRsDeploymentStrategy(patchedSpec, DU_ID1, CLUSTER1).getMaxUnavailable(),
                equalTo(NEW_MAX_UNAVAILABLE));
        assertThat(getRsDeploymentStrategy(patchedSpec, DU_ID1, CLUSTER2).getMaxUnavailable(),
                equalTo(MAX_UNAVAILABLE));
        assertThat(getMcrsDeploymentStrategy(patchedSpec, DU_ID2).getMaxUnavailable(),
                equalTo(MAX_UNAVAILABLE));

        TStageSpec specToCheck = patchMaxUnavailable(STAGE, DU_ID1, Optional.of(CLUSTER1), NEW_MAX_UNAVAILABLE);

        assertThat(patchedSpec, equalTo(specToCheck));
    }

    @Test
    public void testFullOverrideInRsDeployUnit() {
        Map<String, DeployUnitOverrides> deployUnitOverrides = Map.of(DU_ID1, new DeployUnitOverrides(Map.of(
                CLUSTER1, new DeployUnitOverrides.PerClusterOverrides(NEW_MAX_UNAVAILABLE),
                CLUSTER2, new DeployUnitOverrides.PerClusterOverrides(NEW_MAX_UNAVAILABLE)),
                1));
        DeployUnitsOverridesPatcher patcher = new DeployUnitsOverridesPatcher(deployUnitOverrides);
        TStageSpec.Builder stageBuilder = STAGE.toBuilder();
        patcher.patch(stageBuilder);
        TStageSpec patchedSpec = stageBuilder.build();

        assertThat(getRsDeploymentStrategy(patchedSpec, DU_ID1, CLUSTER1).getMaxUnavailable(),
                equalTo(NEW_MAX_UNAVAILABLE));
        assertThat(getRsDeploymentStrategy(patchedSpec, DU_ID1, CLUSTER2).getMaxUnavailable(),
                equalTo(NEW_MAX_UNAVAILABLE));
        assertThat(getMcrsDeploymentStrategy(patchedSpec, DU_ID2).getMaxUnavailable(),
                equalTo(MAX_UNAVAILABLE));

        TStageSpec specToCheck = patchMaxUnavailable(STAGE, DU_ID1, Optional.of(CLUSTER1), NEW_MAX_UNAVAILABLE);
        specToCheck = patchMaxUnavailable(specToCheck, DU_ID1, Optional.of(CLUSTER2), NEW_MAX_UNAVAILABLE);

        assertThat(patchedSpec, equalTo(specToCheck));
    }

    @Test
    public void testFullOverrideInMcrsDeployUnit() {
        Map<String, DeployUnitOverrides> deployUnitOverrides = Map.of(DU_ID2, new DeployUnitOverrides(Map.of(
                MCRS_CLUSTER, new DeployUnitOverrides.PerClusterOverrides(NEW_MAX_UNAVAILABLE)), 1));
        DeployUnitsOverridesPatcher patcher = new DeployUnitsOverridesPatcher(deployUnitOverrides);
        TStageSpec.Builder stageBuilder = STAGE.toBuilder();
        patcher.patch(stageBuilder);
        TStageSpec patchedSpec = stageBuilder.build();

        assertThat(getRsDeploymentStrategy(patchedSpec, DU_ID1, CLUSTER1).getMaxUnavailable(),
                equalTo(MAX_UNAVAILABLE));
        assertThat(getRsDeploymentStrategy(patchedSpec, DU_ID1, CLUSTER2).getMaxUnavailable(),
                equalTo(MAX_UNAVAILABLE));
        assertThat(getMcrsDeploymentStrategy(patchedSpec, DU_ID2).getMaxUnavailable(), equalTo(NEW_MAX_UNAVAILABLE));

        TStageSpec specToCheck = patchMaxUnavailable(STAGE, DU_ID2, Optional.empty(), NEW_MAX_UNAVAILABLE);

        assertThat(patchedSpec, equalTo(specToCheck));
    }

    @Test
    public void testOverrideFewDeployUnits() {
        int anotherNewMaxUnavailable = 10;
        Map<String, DeployUnitOverrides> deployUnitOverrides = Map.of(
                DU_ID1, new DeployUnitOverrides(Map.of(
                        CLUSTER1, new DeployUnitOverrides.PerClusterOverrides(anotherNewMaxUnavailable)), 1),
                DU_ID2, new DeployUnitOverrides(Map.of(
                        MCRS_CLUSTER, new DeployUnitOverrides.PerClusterOverrides(NEW_MAX_UNAVAILABLE)), 1));
        DeployUnitsOverridesPatcher patcher = new DeployUnitsOverridesPatcher(deployUnitOverrides);
        TStageSpec.Builder stageBuilder = STAGE.toBuilder();
        patcher.patch(stageBuilder);
        TStageSpec patchedSpec = stageBuilder.build();

        assertThat(getRsDeploymentStrategy(patchedSpec, DU_ID1, CLUSTER1).getMaxUnavailable(),
                equalTo(anotherNewMaxUnavailable));
        assertThat(getRsDeploymentStrategy(patchedSpec, DU_ID1, CLUSTER2).getMaxUnavailable(),
                equalTo(MAX_UNAVAILABLE));
        assertThat(getMcrsDeploymentStrategy(patchedSpec, DU_ID2).getMaxUnavailable(), equalTo(NEW_MAX_UNAVAILABLE));

        TStageSpec specToCheck = patchMaxUnavailable(STAGE, DU_ID2, Optional.empty(), NEW_MAX_UNAVAILABLE);
        specToCheck = patchMaxUnavailable(specToCheck, DU_ID1, Optional.of(CLUSTER1), anotherNewMaxUnavailable);

        assertThat(patchedSpec, equalTo(specToCheck));
    }

    @Test
    public void testOverrideUnknownDeployUnit() {
        Map<String, DeployUnitOverrides> deployUnitOverrides = Map.of("unknown", new DeployUnitOverrides(Map.of(
                MCRS_CLUSTER, new DeployUnitOverrides.PerClusterOverrides(NEW_MAX_UNAVAILABLE),
                CLUSTER1, new DeployUnitOverrides.PerClusterOverrides(NEW_MAX_UNAVAILABLE)), 1));
        DeployUnitsOverridesPatcher patcher = new DeployUnitsOverridesPatcher(deployUnitOverrides);
        TStageSpec.Builder stageBuilder = STAGE.toBuilder();
        patcher.patch(stageBuilder);
        TStageSpec patchedSpec = stageBuilder.build();

        assertThat(patchedSpec, equalTo(STAGE));
    }

    @Test
    public void testOverrideUnknownCluster() {
        Map<String, DeployUnitOverrides> deployUnitOverrides = Map.of(DU_ID1, new DeployUnitOverrides(Map.of(
                MCRS_CLUSTER, new DeployUnitOverrides.PerClusterOverrides(NEW_MAX_UNAVAILABLE),
                "unknown", new DeployUnitOverrides.PerClusterOverrides(NEW_MAX_UNAVAILABLE)), 1));
        DeployUnitsOverridesPatcher patcher = new DeployUnitsOverridesPatcher(deployUnitOverrides);
        TStageSpec.Builder stageBuilder = STAGE.toBuilder();
        patcher.patch(stageBuilder);
        TStageSpec patchedSpec = stageBuilder.build();

        assertThat(patchedSpec, equalTo(STAGE));
    }

    @Test
    public void testOverrideWithDifferentRevision() {
        Map<String, DeployUnitOverrides> deployUnitOverrides = Map.of(
                DU_ID1, new DeployUnitOverrides(Map.of(
                        CLUSTER1, new DeployUnitOverrides.PerClusterOverrides(NEW_MAX_UNAVAILABLE)), 2),
                DU_ID2, new DeployUnitOverrides(Map.of(
                        MCRS_CLUSTER, new DeployUnitOverrides.PerClusterOverrides(NEW_MAX_UNAVAILABLE)), 2));
        DeployUnitsOverridesPatcher patcher = new DeployUnitsOverridesPatcher(deployUnitOverrides);
        TStageSpec.Builder stageBuilder = STAGE.toBuilder();
        patcher.patch(stageBuilder);
        TStageSpec patchedSpec = stageBuilder.build();

        assertThat(patchedSpec, equalTo(STAGE));
    }
}

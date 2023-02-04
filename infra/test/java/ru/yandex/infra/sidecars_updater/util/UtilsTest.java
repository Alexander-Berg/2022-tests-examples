package ru.yandex.infra.sidecars_updater.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.infra.sidecars_updater.TestUtils;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeListNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeMapNodeImpl;
import ru.yandex.inside.yt.kosher.impl.ytree.YTreeStringNodeImpl;
import ru.yandex.inside.yt.kosher.ytree.YTreeListNode;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.yp.client.api.DataModel;
import ru.yandex.yp.client.api.TCoredumpAggregator;
import ru.yandex.yp.client.api.TCoredumpPolicy;
import ru.yandex.yp.client.api.TCoredumpProcessor;
import ru.yandex.yp.client.api.TDeployUnitSpec;
import ru.yandex.yp.client.api.TInfraComponents;
import ru.yandex.yp.client.api.TLogbrokerConfig;
import ru.yandex.yp.client.api.TMonitoringInfo;
import ru.yandex.yp.client.api.TMonitoringUnistatEndpoint;
import ru.yandex.yp.client.api.TMonitoringWorkloadEndpoint;
import ru.yandex.yp.client.api.TMultiClusterReplicaSetSpec;
import ru.yandex.yp.client.api.TPodAgentMonitoringSpec;
import ru.yandex.yp.client.api.TStageSpec;
import ru.yandex.yp.client.api.TStageSpec.TDeployUnitSettings.TAlerting.EState;
import ru.yandex.yp.client.pods.ECgroupFsMountMode;
import ru.yandex.yp.client.pods.ELayerSourceFileStoragePolicy;
import ru.yandex.yp.client.pods.EResourceAccessPermissions;
import ru.yandex.yp.client.pods.ETransmitSystemLogs;
import ru.yandex.yp.client.pods.EVolumeCreateMode;
import ru.yandex.yp.client.pods.EVolumeMountMode;
import ru.yandex.yp.client.pods.EVolumePersistenceType;
import ru.yandex.yp.client.pods.SecretSelector;
import ru.yandex.yp.client.pods.TBox;
import ru.yandex.yp.client.pods.TComputeResources;
import ru.yandex.yp.client.pods.TFile;
import ru.yandex.yp.client.pods.TFiles;
import ru.yandex.yp.client.pods.TGenericVolume;
import ru.yandex.yp.client.pods.TLayer;
import ru.yandex.yp.client.pods.TMountedVolume;
import ru.yandex.yp.client.pods.TPodAgentSpec;
import ru.yandex.yp.client.pods.TResource;
import ru.yandex.yp.client.pods.TRootfsVolume;
import ru.yandex.yp.client.pods.TTransmitSystemLogsPolicy;
import ru.yandex.yp.client.pods.TVolume;
import ru.yandex.yp.client.pods.TVolumeMountedStaticResource;
import ru.yandex.yp.client.pods.TWorkload;

import static ru.yandex.infra.sidecars_updater.TestUtils.DEFAULT_DEPLOY_UNIT_ID;
import static ru.yandex.infra.sidecars_updater.TestUtils.createStageWithSox;
import static ru.yandex.infra.sidecars_updater.TestUtils.createStageYpObject;
import static ru.yandex.infra.sidecars_updater.TestUtils.getPodBuilder;
import static ru.yandex.infra.sidecars_updater.TestUtils.getResourceRequest;
import static ru.yandex.infra.sidecars_updater.statistics.StatisticsRepository.CLUSTERS;
import static ru.yandex.infra.sidecars_updater.statistics.StatisticsRepository.HDD;
import static ru.yandex.infra.sidecars_updater.statistics.StatisticsRepository.SSD;
import static ru.yandex.yp.client.api.TStageSpec.TDeployUnitSettings.TAlerting.EState.ENABLED;
import static ru.yandex.yp.client.api.TStageSpec.TDeployUnitSettings.TAlerting.EState.IDLE;
import static ru.yandex.yp.client.api.TStageSpec.TDeployUnitSettings.TAlerting.EState.REMOVED;
import static ru.yandex.yp.client.api.TStageSpec.TDeployUnitSettings.TAlerting.EState.UNKNOWN;
import static ru.yandex.yp.client.pods.ECgroupFsMountMode.ECgroupFsMountMode_NONE;
import static ru.yandex.yp.client.pods.ECgroupFsMountMode.ECgroupFsMountMode_RO;
import static ru.yandex.yp.client.pods.ECgroupFsMountMode.ECgroupFsMountMode_RW;
import static ru.yandex.yp.client.pods.EResourceAccessPermissions.EResourceAccessPermissions_600;
import static ru.yandex.yp.client.pods.EResourceAccessPermissions.EResourceAccessPermissions_660;
import static ru.yandex.yp.client.pods.EResourceAccessPermissions.EResourceAccessPermissions_UNMODIFIED;

class UtilsTest {
    public static Stream<Arguments> usageClustersSource() {
        return Stream.of(
                Arguments.of(List.of("man", "vla"), true),
                Arguments.of(List.of("man", "vla"), false),
                Arguments.of(CLUSTERS, true),
                Arguments.of(CLUSTERS, false),
                Arguments.of(List.of("sas"), true),
                Arguments.of(List.of("sas"), false),
                Arguments.of(Collections.emptyList(), true),
                Arguments.of(Collections.emptyList(), false),
                Arguments.of(List.of("iva", "myt"), true),
                Arguments.of(List.of("iva", "myt"), false)
        );
    }

    @ParameterizedTest
    @MethodSource("usageClustersSource")
    public void usageClustersTest(List<String> clusters, boolean isMultiClusterReplicaSet) {
        var deployUnitBuilder = TDeployUnitSpec.newBuilder();
        if (isMultiClusterReplicaSet) {
            deployUnitBuilder.setMultiClusterReplicaSet(TDeployUnitSpec.TMultiClusterReplicaSetDeploy.newBuilder());
            var clustersWithType =
                    clusters.stream()
                            .map(it -> TMultiClusterReplicaSetSpec.TClusterReplicaSetSpecPreferences.newBuilder()
                                    .setCluster(it)
                                    .build())
                            .collect(Collectors.toList());
            deployUnitBuilder.getMultiClusterReplicaSetBuilder()
                    .getReplicaSetBuilder()
                    .addAllClusters(clustersWithType);
        } else {
            deployUnitBuilder.setReplicaSet(TDeployUnitSpec.TReplicaSetDeploy.newBuilder());
            Map<String, TDeployUnitSpec.TReplicaSetDeploy.TPerClusterSettings> map = new HashMap<>();
            for (String cluster : clusters) {
                map.put(cluster, TDeployUnitSpec.TReplicaSetDeploy.TPerClusterSettings.newBuilder().build());
            }
            deployUnitBuilder.getReplicaSetBuilder().putAllPerClusterSettings(map);
        }
        TDeployUnitSpec deployUnitSpec = deployUnitBuilder.build();
        for (String cluster : CLUSTERS) {
            int result = Utils.countClusterInDuSpec(deployUnitSpec, cluster);
            if (clusters.contains(cluster)) {
                Assertions.assertEquals(result, 1);
            } else {
                Assertions.assertEquals(result, 0);
            }
        }
    }


    public static Stream<Arguments> disabledClustersSource() {
        return Stream.of(
                Arguments.of(List.of("man", "vla"), CLUSTERS, true),
                Arguments.of(List.of("man", "vla"), CLUSTERS, false),
                Arguments.of(CLUSTERS, CLUSTERS, true),
                Arguments.of(CLUSTERS, CLUSTERS, false),
                Arguments.of(CLUSTERS, List.of("sas"), true),
                Arguments.of(CLUSTERS, List.of("sas"), false),
                Arguments.of(Collections.emptyList(), Collections.emptyList(), true),
                Arguments.of(Collections.emptyList(), Collections.emptyList(), false),
                Arguments.of(List.of("iva", "myt"), List.of("iva", "man"), true),
                Arguments.of(List.of("iva", "myt"), List.of("iva", "man"), false)
        );
    }

    @ParameterizedTest
    @MethodSource("disabledClustersSource")
    public void disabledClustersTest(List<String> disabledClusters, List<String> enabledClusters,
                                     boolean isMultiClusterReplicaSet) {
        Map<String, YTreeNode> stageLabels = new HashMap<>();
        YTreeListNode node = new YTreeListNodeImpl(null);
        node.addAll(disabledClusters.stream().map(it -> new YTreeStringNodeImpl(it, null)).collect(Collectors.toList()));
        stageLabels.put(Utils.DEPLOY, new YTreeMapNodeImpl(Map.of(Utils.DISABLED_CLUSTERS, node), null));
        var deployUnitBuilder = TDeployUnitSpec.newBuilder();
        if (isMultiClusterReplicaSet) {
            deployUnitBuilder.setMultiClusterReplicaSet(TDeployUnitSpec.TMultiClusterReplicaSetDeploy.newBuilder());
            var clustersWithType =
                    enabledClusters.stream()
                            .map(it -> TMultiClusterReplicaSetSpec.TClusterReplicaSetSpecPreferences.newBuilder()
                                    .setCluster(it)
                                    .build())
                            .collect(Collectors.toList());
            deployUnitBuilder.getMultiClusterReplicaSetBuilder()
                    .getReplicaSetBuilder()
                    .addAllClusters(clustersWithType);
        } else {
            deployUnitBuilder.setReplicaSet(TDeployUnitSpec.TReplicaSetDeploy.newBuilder());
            Map<String, TDeployUnitSpec.TReplicaSetDeploy.TPerClusterSettings> map = new HashMap<>();
            for (String cluster : enabledClusters) {
                map.put(cluster, TDeployUnitSpec.TReplicaSetDeploy.TPerClusterSettings.newBuilder().build());
            }
            deployUnitBuilder.getReplicaSetBuilder().putAllPerClusterSettings(map);
        }
        TDeployUnitSpec deployUnitSpec = deployUnitBuilder.build();
        for (String cluster : CLUSTERS) {
            int result = Utils.countDisabledCluster(new TStageAndDuId(createStageYpObject(deployUnitSpec, stageLabels),
                    DEFAULT_DEPLOY_UNIT_ID), cluster);
            if (enabledClusters.contains(cluster) && disabledClusters.contains(cluster)) {
                Assertions.assertEquals(result, 1);
            } else {
                Assertions.assertEquals(result, 0);
            }
        }
    }

    public static Stream<Arguments> isDeployUnitUseLogsSource() {
        return Stream.of(
                Arguments.of(false, false, List.of(), false),
                Arguments.of(true, false, List.of(), true),
                Arguments.of(false, true, List.of(), true),
                Arguments.of(true, true, List.of(), true),
                Arguments.of(false, false, List.of(true), true),
                Arguments.of(false, true, List.of(true), true),
                Arguments.of(false, false, List.of(false, false), false),
                Arguments.of(false, false, List.of(true, true), true),
                Arguments.of(false, false, List.of(false, true), true),
                Arguments.of(false, false, List.of(false, false, false), false),
                Arguments.of(false, false, List.of(true, false, false), true),
                Arguments.of(true, false, List.of(true, false, false), true),
                Arguments.of(false, true, List.of(true, false, true), true),
                Arguments.of(true, true, List.of(true, false, true), true)
        );
    }

    @ParameterizedTest
    @MethodSource("isDeployUnitUseLogsSource")
    public void isDeployUnitUseLogsTest(boolean isUseSystemLogs, boolean isMandatoryMode,
                                        List<Boolean> transmitLogsOnWorkloads,
                                        boolean isUseLogs) {
        var deployUnitBuilder = TestUtils.createDeployUnitBuilder(false, isMandatoryMode, false, false, false);
        TestUtils.setUpPodToDeployUnitBuilder(deployUnitBuilder);
        List<TWorkload.Builder> workloadBuilders = transmitLogsOnWorkloads.stream().
                map(isTransmitLogs -> TWorkload.newBuilder().setTransmitLogs(isTransmitLogs)).collect(Collectors.toList());
        TestUtils.setWorkloadsToPodAgent(deployUnitBuilder, workloadBuilders);

        if (isUseSystemLogs) {
            getPodBuilder(deployUnitBuilder).getPodAgentPayloadBuilder().getSpecBuilder().setTransmitSystemLogsPolicy(TTransmitSystemLogsPolicy.newBuilder()
                    .setTransmitSystemLogs(ETransmitSystemLogs.ETransmitSystemLogsPolicy_ENABLED)
                    .build());
        }

        Assertions.assertEquals(isUseLogs, Utils.isDeployUnitUseLogs(deployUnitBuilder.build()));
    }


    public static Stream<Arguments> isDeployUnitUseSysLogsSource() {
        return Stream.of(
                Arguments.of(ETransmitSystemLogs.ETransmitSystemLogsPolicy_ENABLED, true),
                Arguments.of(ETransmitSystemLogs.ETransmitSystemLogsPolicy_DISABLED, false),
                Arguments.of(ETransmitSystemLogs.ETransmitSystemLogsPolicy_NONE, false)
        );
    }

    @ParameterizedTest
    @MethodSource("isDeployUnitUseSysLogsSource")
    public void isDeployUnitUseSysLogsTest(ETransmitSystemLogs transmitSystemLogs, boolean result) {
        var deployUnitBuilder = TDeployUnitSpec.newBuilder();
        TestUtils.setUpPodToDeployUnitBuilder(deployUnitBuilder);
        getPodBuilder(deployUnitBuilder)
                .setPodAgentPayload(
                        DataModel.TPodSpec.TPodAgentPayload.newBuilder()
                                .setSpec(TPodAgentSpec.newBuilder()
                                        .setTransmitSystemLogsPolicy(TTransmitSystemLogsPolicy.newBuilder()
                                                .setTransmitSystemLogs(transmitSystemLogs)
                                        )
                                )
                );
        Assertions.assertEquals(result, Utils.isDeployUnitUseSysLogs(deployUnitBuilder.build()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public static void isDeployUnitUseCoreDumpTest(boolean enabledCoreDump) {
        var deployUnitBuilder = TestUtils.createDeployUnitBuilder(false, false, false, false, false);

        var workloadIds = List.of("workloadId1", "workloadId2");
        TestUtils.setWorkloadsByIdsToPodAgent(deployUnitBuilder, workloadIds);

        if (enabledCoreDump) {
            deployUnitBuilder.putAllCoredumpConfig(Map.of(workloadIds.get(0), TCoredumpPolicy.getDefaultInstance()));
        }
        Assertions.assertEquals(enabledCoreDump, Utils.isDeployUnitEnableCoreDumps(deployUnitBuilder.build()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public static void isDeployUnitUseCoreDumpAggregationTest(boolean enabledCoreDump) {
        var deployUnitBuilder = TestUtils.createDeployUnitBuilder(false, false, false, false, false);

        List<String> workloadIds = List.of("workloadId1", "workloadId2");
        TestUtils.setWorkloadsByIdsToPodAgent(deployUnitBuilder, workloadIds);

        if (enabledCoreDump) {
            deployUnitBuilder.putAllCoredumpConfig(Map.of(workloadIds.get(0), TCoredumpPolicy.newBuilder()
                    .setCoredumpProcessor(TCoredumpProcessor.newBuilder()
                            .setAggregator(
                                    TCoredumpAggregator.newBuilder().setEnabled(true)
                            )
                    )
                    .build()));
        }
        deployUnitBuilder.putAllCoredumpConfig(Map.of(workloadIds.get(1), TCoredumpPolicy.newBuilder()
                .setCoredumpProcessor(TCoredumpProcessor.newBuilder()
                        .setAggregator(
                                TCoredumpAggregator.newBuilder().setEnabled(false)
                        )
                )
                .build()));
        Assertions.assertEquals(enabledCoreDump, Utils.isDeployUnitEnableCoreDumps(deployUnitBuilder.build()));
    }


    public static Stream<Arguments> isDeployUnitHasLimitsSource() {
        return Stream.of(
                Arguments.of(
                        List.of(),
                        false
                ),
                Arguments.of(
                        List.of(
                                new TestUtils.Limits(0, 0, 0, false, false)
                        ),
                        false
                ),
                Arguments.of(
                        List.of(
                                new TestUtils.Limits(1, 0, 0, false, false)
                        ),
                        true
                ),
                Arguments.of(
                        List.of(
                                new TestUtils.Limits(0, 1, 0, true, false)
                        ),
                        true
                ),
                Arguments.of(
                        List.of(
                                new TestUtils.Limits(0, 1, 0, false, false),
                                new TestUtils.Limits(0, 1, 0, false, false)
                        ),
                        true
                ),
                Arguments.of(
                        List.of(
                                new TestUtils.Limits(0, 0, 0, false, false),
                                new TestUtils.Limits(0, 0, 0, false, false)
                        ),
                        false
                ),
                Arguments.of(
                        List.of(
                                new TestUtils.Limits(1, 1, 1, true, false),
                                new TestUtils.Limits(1, 1, 0, false, false),
                                new TestUtils.Limits(1, 1, 0, false, true)
                        ),
                        true
                ),
                Arguments.of(
                        List.of(
                                new TestUtils.Limits(0, 0, 0, false, false),
                                new TestUtils.Limits(0, 0, 0, false, false),
                                new TestUtils.Limits(0, 0, 0, false, true)
                        ),
                        true
                ),
                Arguments.of(
                        List.of(
                                new TestUtils.Limits(0, 0, 0, false, false),
                                new TestUtils.Limits(0, 0, 0, true, false),
                                new TestUtils.Limits(0, 0, 0, false, false)
                        ),
                        true
                ),
                Arguments.of(
                        List.of(
                                new TestUtils.Limits(0, 1, 0, true, false),
                                new TestUtils.Limits(1, 0, 0, false, true),
                                new TestUtils.Limits(0, 0, 0, false, false)
                        ),
                        true
                )
        );
    }

    @ParameterizedTest
    @MethodSource("isDeployUnitHasLimitsSource")
    public void isDeployUnitHasLimitsOnWorkLoadsTest(List<TestUtils.Limits> workloadLimitsList,
                                                     boolean hasLimits) {
        var deployUnitBuilder = TestUtils.createDeployUnitBuilder(false, false, false, false, false);
        TestUtils.setUpPodToDeployUnitBuilder(deployUnitBuilder);
        List<TWorkload.Builder> workloadBuilders = workloadLimitsList.stream().
                map(workloadLimits -> TestUtils.setWorkloadsLimits(TWorkload.newBuilder(), workloadLimits)).collect(Collectors.toList());
        TestUtils.setWorkloadsToPodAgent(deployUnitBuilder, workloadBuilders);
        Assertions.assertEquals(hasLimits, Utils.isDeployUnitHasLimitsOnWorkLoads(deployUnitBuilder.build()));
    }

    @ParameterizedTest
    @MethodSource("isDeployUnitHasLimitsSource")
    public void isDeployUnitHasBoxLimitsTest(List<TestUtils.Limits> limitsList,
                                             boolean hasLimits) {
        var deployUnitBuilder = TestUtils.createDeployUnitBuilder(false, false, false, false, false);
        TestUtils.setUpPodToDeployUnitBuilder(deployUnitBuilder);
        List<TBox> boxes = limitsList.stream().map(limits -> TBox.newBuilder()
                .setComputeResources(TestUtils.getComputeResourcesWithLimits(limits)).build()).collect(Collectors.toList());
        getPodBuilder(deployUnitBuilder).setPodAgentPayload(DataModel.TPodSpec.TPodAgentPayload.newBuilder()
                .setSpec(TPodAgentSpec.newBuilder().addAllBoxes(boxes)));
        Assertions.assertEquals(hasLimits, Utils.isDeployUnitHasBoxLimits(deployUnitBuilder.build()));
    }

    public static Stream<Arguments> isDeployUnitWithoutGuaranteeSource() {
        return Stream.of(
                Arguments.of(
                        new TestUtils.Guarantees(0, 0, 0),
                        true
                ),
                Arguments.of(
                        new TestUtils.Guarantees(1, 1, 1),
                        false
                )
        );
    }

    @ParameterizedTest
    @MethodSource("isDeployUnitWithoutGuaranteeSource")
    public void isDeployUnitWithoutGuaranteeTest(TestUtils.Guarantees guarantees,
                                                 boolean guaranteeAbsent) {
        var deployUnitBuilder = TestUtils.createDeployUnitBuilder(false, false, false, false, false);
        TestUtils.setUpPodToDeployUnitBuilder(deployUnitBuilder);
        getPodBuilder(deployUnitBuilder).setPodAgentPayload(DataModel.TPodSpec.TPodAgentPayload.newBuilder()
                        .setSpec(TPodAgentSpec.newBuilder().addBoxes(TBox.newBuilder())))
                .setResourceRequests(getResourceRequest(guarantees));
        Assertions.assertEquals(guaranteeAbsent, Utils.isDeployUnitWithoutNetworkGuarantee(deployUnitBuilder.build()));
    }

    public static Stream<Arguments> isDeployUnitWithoutIoLimitAndGuaranteeSource() {
        return Stream.of(
                Arguments.of(
                        List.of(
                                new TestUtils.DiskQuotaPolicy(1, 1),
                                new TestUtils.DiskQuotaPolicy(1, 1)
                        ),
                        false
                ),
                Arguments.of(
                        List.of(
                                new TestUtils.DiskQuotaPolicy(0, 0),
                                new TestUtils.DiskQuotaPolicy(0, 0)
                        ),
                        true
                ),
                Arguments.of(
                        List.of(
                                new TestUtils.DiskQuotaPolicy(0, 0),
                                new TestUtils.DiskQuotaPolicy(1, 1)
                        ),
                        true
                )
        );
    }

    @ParameterizedTest
    @MethodSource("isDeployUnitWithoutIoLimitAndGuaranteeSource")
    public void isDeployUnitWithoutIoRequestTest(List<TestUtils.DiskQuotaPolicy> diskQuotaPolicies,
                                                 boolean guaranteeAbsent) {
        var deployUnitBuilder = TestUtils.createDeployUnitBuilder(false, false, false, false, false);
        TestUtils.setUpPodToDeployUnitBuilder(deployUnitBuilder);

        DataModel.TPodSpec.TDiskVolumeRequest.TQuotaPolicy quotaPolicy1 =
                DataModel.TPodSpec.TDiskVolumeRequest.TQuotaPolicy.newBuilder()
                        .setBandwidthGuarantee(diskQuotaPolicies.get(0).ioGuarantee)
                        .setBandwidthLimit(diskQuotaPolicies.get(0).ioLimit)
                        .setCapacity(123)
                        .build();

        DataModel.TPodSpec.TDiskVolumeRequest.TQuotaPolicy quotaPolicy2 =
                DataModel.TPodSpec.TDiskVolumeRequest.TQuotaPolicy.newBuilder()
                        .setBandwidthGuarantee(diskQuotaPolicies.get(1).ioGuarantee)
                        .setBandwidthLimit(diskQuotaPolicies.get(1).ioLimit)
                        .setCapacity(123)
                        .build();

        DataModel.TPodSpec.Builder builder = getPodBuilder(deployUnitBuilder)
                .setPodAgentPayload(DataModel.TPodSpec.TPodAgentPayload.newBuilder()
                        .setSpec(TPodAgentSpec.newBuilder()
                                .addBoxes(TBox.newBuilder())));

        diskQuotaPolicies.stream()
                .map(diskQuotaPolicy -> DataModel.TPodSpec.TDiskVolumeRequest.TQuotaPolicy.newBuilder()
                        .setBandwidthGuarantee(diskQuotaPolicy.ioGuarantee)
                        .setBandwidthLimit(diskQuotaPolicy.ioLimit)
                        .setCapacity(123)
                        .build())
                .forEach(quotaPolicy -> ImmutableList.of(HDD, SSD)
                        .forEach(storageClass -> builder.addDiskVolumeRequests(DataModel.TPodSpec.TDiskVolumeRequest.newBuilder()
                                .setId(UUID.randomUUID().toString())
                                .setQuotaPolicy(quotaPolicy)
                                .setStorageClass(storageClass)
                                .build())
                        )
                );

        Assertions.assertEquals(guaranteeAbsent, Utils.isDeployUnitWithoutIoGuarantee(deployUnitBuilder.build(), SSD));
        Assertions.assertEquals(guaranteeAbsent, Utils.isDeployUnitWithoutIoLimit(deployUnitBuilder.build(), SSD));
        Assertions.assertEquals(guaranteeAbsent, Utils.isDeployUnitWithoutIoGuarantee(deployUnitBuilder.build(), HDD));
        Assertions.assertEquals(guaranteeAbsent, Utils.isDeployUnitWithoutIoLimit(deployUnitBuilder.build(), HDD));
    }

    public static Stream<Arguments> isDeployUnitHasInheritSource() {
        return Stream.of(
                Arguments.of(
                        List.of(),
                        List.of(),
                        false
                ),
                Arguments.of(
                        List.of(false),
                        List.of(),
                        false
                ),
                Arguments.of(
                        List.of(true),
                        List.of(),
                        true
                ),
                Arguments.of(
                        List.of(),
                        List.of(false),
                        false
                ),
                Arguments.of(
                        List.of(),
                        List.of(true),
                        true
                ),
                Arguments.of(
                        List.of(true),
                        List.of(true),
                        true
                ),
                Arguments.of(
                        List.of(true, false, true),
                        List.of(false, true),
                        true
                ),
                Arguments.of(
                        List.of(false, false, false),
                        List.of(false, false),
                        false
                ),
                Arguments.of(
                        List.of(false, false, false),
                        List.of(false, true),
                        true
                ),
                Arguments.of(
                        List.of(false, true, false),
                        List.of(false, false, true),
                        true
                )
        );
    }

    @ParameterizedTest
    @MethodSource("isDeployUnitHasInheritSource")
    public void isDeployUnitHasInheritTest(List<Boolean> workloadMissedLabelsList,
                                           List<Boolean> unistatMissedLabelsList, boolean hasInherit) {
        var deployUnitBuilder = TestUtils.createDeployUnitBuilder(false, false, false, false, false);
        TestUtils.setUpPodToDeployUnitBuilder(deployUnitBuilder);
        TestUtils.setUpMonitoringToDeployUnitBuilder(deployUnitBuilder);
        List<TMonitoringWorkloadEndpoint.Builder> workloadBuilders =
                workloadMissedLabelsList.stream().map(isMissedLabels ->
                        TMonitoringWorkloadEndpoint.newBuilder().setInheritMissedLabels(isMissedLabels)).collect(Collectors.toList());
        List<TMonitoringUnistatEndpoint.Builder> unistatBuilders =
                unistatMissedLabelsList.stream().map(isMissedLabels ->
                        TMonitoringUnistatEndpoint.newBuilder().setInheritMissedLabels(isMissedLabels)).collect(Collectors.toList());
        TMonitoringInfo.Builder monitoringInfoBuilder = getPodBuilder(deployUnitBuilder).getHostInfraBuilder()
                .getMonitoringBuilder();
        workloadBuilders.forEach(monitoringInfoBuilder::addWorkloads);
        unistatBuilders.forEach(monitoringInfoBuilder::addUnistats);
        Assertions.assertEquals(hasInherit,
                Utils.isDeployUnitHasInheritMissedMonitoringLabels(deployUnitBuilder.build()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void isDeployUnitHasUserSignalsTest(boolean hasUserSignals) {
        var deployUnitBuilder = TestUtils.createDeployUnitBuilder(false, false, false, false, false);
        TestUtils.setUpPodToDeployUnitBuilder(deployUnitBuilder);
        TestUtils.setUpMonitoringToDeployUnitBuilder(deployUnitBuilder);
        getPodBuilder(deployUnitBuilder).getHostInfraBuilder().getMonitoringBuilder().setPodAgent(
                TPodAgentMonitoringSpec.newBuilder().setAddPodAgentUserSignals(hasUserSignals));
        Assertions.assertEquals(hasUserSignals, Utils.isDeployUnitHasUserSignals(deployUnitBuilder.build()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public static void isDeployUnitHasDockerTest(boolean hasDocker) {
        var deployUnitBuilder = TestUtils.createDeployUnitBuilder(false, false, hasDocker, false, false);
        Assertions.assertEquals(hasDocker, Utils.isDeployUnitHasDocker(deployUnitBuilder.build()));
    }


    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void isSetNetworkBandwidthGuaranteeTest(boolean isSetNetworkBandwidthGuarantee) {
        var deployUnitBuilder = TestUtils.createDefaultDeployUnit();
        TestUtils.setUpPodToDeployUnitBuilder(deployUnitBuilder);
        DataModel.TPodSpec.Builder podBuilder = deployUnitBuilder.getReplicaSetBuilder().getReplicaSetTemplateBuilder()
                .getPodTemplateSpecBuilder().getSpecBuilder();
        DataModel.TPodSpec.TResourceRequests.Builder resourceRequestBuilder = DataModel.TPodSpec.
                TResourceRequests.newBuilder()
                .setNetworkBandwidthGuarantee(isSetNetworkBandwidthGuarantee ? 1 : 0);
        podBuilder.setResourceRequests(resourceRequestBuilder);
        Assertions.assertEquals(isSetNetworkBandwidthGuarantee,
                Utils.isSetNetworkBandwidthGuarantee(deployUnitBuilder.build()));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 5, 10, 20})
    public void getBoxAmountTest(int boxAmount) {
        var deployUnitBuilder = TDeployUnitSpec.newBuilder();
        TestUtils.addBoxesToDeployUnitBuilder(deployUnitBuilder, boxAmount);
        Assertions.assertEquals(boxAmount, Utils.getBoxAmount(deployUnitBuilder.build()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void isDeployUnitAllowAutoSidecarsUpdate(boolean isAllowAutoSidecarsUpdate) {
        var deployUnitBuilder = TestUtils.createDefaultDeployUnit()
                .setInfraComponents(TInfraComponents.newBuilder()
                        .setAllowAutomaticUpdates(isAllowAutoSidecarsUpdate));
        Assertions.assertEquals(isAllowAutoSidecarsUpdate,
                Utils.isDeployUnitAllowAutoSidecarsUpdate(deployUnitBuilder.build()));
    }

    public static Stream<Arguments> isDeployUnitHasLowGuaranteeForUnifiedAgentSource() {
        return Stream.of(
                Arguments.of(-1, -1, false),
                Arguments.of(0, 0, true),
                Arguments.of(0, 1, false),
                Arguments.of(1, 0, false),
                Arguments.of(1, 1, false)
        );
    }

    @ParameterizedTest
    @MethodSource("isDeployUnitHasLowGuaranteeForUnifiedAgentSource")
    public void isDeployUnitHasLowGuaranteeForUnifiedAgentTest(int vcpuGuarantee, int vcpuLimit,
                                                               boolean isLowGuaranteeForUnifiedAgentSource) {
        TDeployUnitSpec.Builder deployUnitBuilder;
        if (vcpuGuarantee == -1 || vcpuLimit == -1) {
            deployUnitBuilder = TDeployUnitSpec.newBuilder();
        } else {
            deployUnitBuilder = TestUtils.createDefaultDeployUnit()
                    .setLogbrokerConfig(TLogbrokerConfig.newBuilder()
                            .setPodAdditionalResourcesRequest(DataModel.TPodSpec.TResourceRequests.newBuilder()
                                    .setVcpuGuarantee(vcpuGuarantee)
                                    .setVcpuLimit(vcpuLimit)));
        }
        Assertions.assertEquals(isLowGuaranteeForUnifiedAgentSource,
                Utils.isDeployUnitHasLowGuaranteeForUnifiedAgent(deployUnitBuilder.build()));
    }

    public static Stream<Arguments> isDeployUnitHasCGroupFsReadOnlyModeSource() {
        return Stream.of(
                Arguments.of(List.of(ECgroupFsMountMode_RO), true),
                Arguments.of(List.of(ECgroupFsMountMode_NONE), false),
                Arguments.of(List.of(ECgroupFsMountMode_RW), false),
                Arguments.of(List.of(), false),
                Arguments.of(List.of(ECgroupFsMountMode_NONE, ECgroupFsMountMode_RO), true),
                Arguments.of(List.of(ECgroupFsMountMode_RO, ECgroupFsMountMode_RO), true),
                Arguments.of(List.of(ECgroupFsMountMode_RW, ECgroupFsMountMode_NONE), false)
        );
    }

    @ParameterizedTest
    @MethodSource("isDeployUnitHasCGroupFsReadOnlyModeSource")
    public void isDeployUnitHasCGroupFsReadOnlyModeTest(List<ECgroupFsMountMode> boxECgroupFsMountModes,
                                                        boolean isReadOnlyMode) {
        var boxes = boxECgroupFsMountModes.stream()
                .map(mode -> TBox.newBuilder().setCgroupFsMountMode(mode).build())
                .collect(Collectors.toList());
        var deployUnitBuilder = TDeployUnitSpec.newBuilder();
        TestUtils.setUpPodToDeployUnitBuilder(deployUnitBuilder);
        deployUnitBuilder.getReplicaSetBuilder().getReplicaSetTemplateBuilder().getPodTemplateSpecBuilder().getSpecBuilder()
                .setPodAgentPayload(DataModel.TPodSpec.TPodAgentPayload.newBuilder()
                        .setSpec(TPodAgentSpec.newBuilder().addAllBoxes(boxes)));
        Assertions.assertEquals(isReadOnlyMode, Utils.isDeployUnitHasCGroupFsReadOnlyMode(deployUnitBuilder.build()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void isSoxStageTest(boolean isSox) {
        var stage = TStageSpec.newBuilder().setSoxService(isSox).build();
        Assertions.assertEquals(isSox, Utils.isSoxStage(stage));
    }

    public static Stream<Arguments> isAlertsEnabledSource() {
        return Stream.of(
                Arguments.of(ENABLED, true),
                Arguments.of(REMOVED, false),
                Arguments.of(IDLE, false),
                Arguments.of(UNKNOWN, false)
        );
    }

    @ParameterizedTest
    @MethodSource("isAlertsEnabledSource")
    public void isAlertsEnabled(EState state, boolean isEnables) {
        var stage = TStageSpec.newBuilder().putDeployUnitSettings("deployUnit",
                TStageSpec.TDeployUnitSettings.newBuilder()
                        .setAlerting(TStageSpec.TDeployUnitSettings.TAlerting.newBuilder().setState(state))
                        .build()).build();
        Assertions.assertEquals(isEnables, Utils.isDeployUnitHasAlertsEnabled(stage));
    }

    public static Stream<Arguments> countEnvironmentByTypeSource() {
        return Stream.of(
                Arguments.of(TStageSpec.TDeployUnitSettings.EDeployUnitEnvironment.UNKNOWN, 2),
                Arguments.of(TStageSpec.TDeployUnitSettings.EDeployUnitEnvironment.TESTING, 1),
                Arguments.of(TStageSpec.TDeployUnitSettings.EDeployUnitEnvironment.PRESTABLE, 1),
                Arguments.of(TStageSpec.TDeployUnitSettings.EDeployUnitEnvironment.STABLE, 1)
        );
    }

    @ParameterizedTest
    @MethodSource("countEnvironmentByTypeSource")
    public void countEnvironmentByType(TStageSpec.TDeployUnitSettings.EDeployUnitEnvironment env, int count) {
        var stage = TStageSpec.newBuilder().putDeployUnitSettings("deployUnit",
                        TStageSpec.TDeployUnitSettings.newBuilder()
                                .setEnvironment(env)
                                .build())
                .putDeployUnitSettings("deployUnit2", TStageSpec.TDeployUnitSettings.newBuilder().build())
                .build();
        var func = Utils.countEnvironmentByType(env);
        Assertions.assertEquals(count, func.apply(stage));
    }

    public static Stream<Arguments> isDeployUnitHasSecretEnvAndChildOnlyIsolationSource() {
        return Stream.of(
                Arguments.of(1, true, false),
                Arguments.of(1, false, false),
                Arguments.of(7, false, false),
                Arguments.of(7, true, true),
                Arguments.of(6, true, false),
                Arguments.of(8, true, true),
                Arguments.of(8, false, false)
        );
    }

    @ParameterizedTest
    @MethodSource("isDeployUnitHasSecretEnvAndChildOnlyIsolationSource")
    public void isDeployUnitHasSecretEnvAndChildOnlyIsolationTest(int patchersRevision, boolean soxService,
                                                                  boolean result) {
        var deployUnit = TDeployUnitSpec.newBuilder()
                .setPatchersRevision(patchersRevision)
                .setSoxService(soxService)
                .build();
        Assertions.assertEquals(result, Utils.isDeployUnitHasSecretEnvAndChildOnlyIsolation(deployUnit));
    }


    public static Stream<Arguments> isDeployUnitHasSecureRightsToStaticResourcesSource() {
        return Stream.of(
                Arguments.of(List.of(), false),
                Arguments.of(List.of(EResourceAccessPermissions.EResourceAccessPermissions_UNMODIFIED), false),
                Arguments.of(List.of(EResourceAccessPermissions_600), true),
                Arguments.of(List.of(EResourceAccessPermissions.EResourceAccessPermissions_660), true),
                Arguments.of(List.of(
                        EResourceAccessPermissions.EResourceAccessPermissions_UNMODIFIED,
                        EResourceAccessPermissions.EResourceAccessPermissions_UNMODIFIED
                ), false)
        );
    }

    @ParameterizedTest
    @MethodSource("isDeployUnitHasSecureRightsToStaticResourcesSource")
    public void isDeployUnitHasSecureRightsToStaticResourcesTest(
            List<EResourceAccessPermissions> staticResourceAccessPermissions, boolean result) {
        var staticResources = staticResourceAccessPermissions.stream()
                .map(accessPermissions -> TResource.newBuilder().setAccessPermissions(accessPermissions).build())
                .collect(Collectors.toList());

        var deployUnitBuilder = TDeployUnitSpec.newBuilder();
        TestUtils.setUpPodToDeployUnitBuilder(deployUnitBuilder);
        deployUnitBuilder.getReplicaSetBuilder()
                .getReplicaSetTemplateBuilder()
                .getPodTemplateSpecBuilder()
                .getSpecBuilder()
                .getPodAgentPayloadBuilder()
                .getSpecBuilder()
                .getResourcesBuilder()
                .addAllStaticResources(staticResources);

        Assertions.assertEquals(result, Utils.isDeployUnitHasSecureRightsToStaticResources(deployUnitBuilder.build()));
    }


    public static Stream<Arguments> isDeployUnitHasConfigsAndBinariesViaVolumeMountedToBoxSource() {
        return Stream.of(
                Arguments.of(List.of(), List.of(), false),
                Arguments.of(
                        List.of(new TestUtils.Volume("1", 1, 1)),
                        List.of(new TestUtils.MountedVolume("1", EVolumeMountMode.EVolumeMountMode_READ_ONLY)),
                        true),
                Arguments.of(
                        List.of(new TestUtils.Volume("1", 1, 1)),
                        List.of(new TestUtils.MountedVolume("1", EVolumeMountMode.EVolumeMountMode_READ_WRITE)),
                        false),
                Arguments.of(
                        List.of(
                                new TestUtils.Volume("1", 0, 0),
                                new TestUtils.Volume("2", 1, 1)
                        ),
                        List.of(
                                new TestUtils.MountedVolume("1", EVolumeMountMode.EVolumeMountMode_READ_ONLY),
                                new TestUtils.MountedVolume("2", EVolumeMountMode.EVolumeMountMode_READ_WRITE)
                        ),
                        false)
        );
    }

    @ParameterizedTest
    @MethodSource("isDeployUnitHasConfigsAndBinariesViaVolumeMountedToBoxSource")
    public void isDeployUnitHasConfigsAndBinariesViaVolumeMountedToBoxTest(List<TestUtils.Volume> volumes,
                                                                           List<TestUtils.MountedVolume> mountedVolumes,
                                                                           boolean result) {
        var box = TBox.newBuilder().addAllVolumes(
                        mountedVolumes.stream()
                                .map(volume -> TMountedVolume.newBuilder()
                                        .setMode(volume.mountMode)
                                        .setVolumeRef(volume.ref)
                                        .build()).collect(Collectors.toList()))
                .build();

        var deployUnitBuilder = TDeployUnitSpec.newBuilder();
        TestUtils.setUpPodToDeployUnitBuilder(deployUnitBuilder);
        deployUnitBuilder.getReplicaSetBuilder()
                .getReplicaSetTemplateBuilder()
                .getPodTemplateSpecBuilder()
                .getSpecBuilder()
                .getPodAgentPayloadBuilder()
                .getSpecBuilder()
                .addBoxes(box)
                .addAllVolumes(
                        volumes.stream()
                                .map(volume -> TVolume.newBuilder()
                                        .setId(volume.id)
                                        .addAllStaticResources(
                                                Collections.nCopies(volume.staticResourcesCount,
                                                        TVolumeMountedStaticResource.newBuilder().build())
                                        )
                                        .setGeneric(
                                                TGenericVolume.newBuilder()
                                                        .addAllLayerRefs(
                                                                Collections.nCopies(volume.layerRefsCount, "-1")
                                                        )
                                        )
                                        .build())
                                .collect(Collectors.toList())
                );

        Assertions.assertEquals(result,
                Utils.isDeployUnitHasConfigsAndBinariesViaVolumeMountedToBox(deployUnitBuilder.build()));
    }


    public static Stream<Arguments> isDeployUnitHasReadOnlyBoxRootfsSource() {
        return Stream.of(
                Arguments.of(7, List.of(EVolumeCreateMode.EVolumeCreateMode_READ_ONLY), true),
                Arguments.of(6, List.of(EVolumeCreateMode.EVolumeCreateMode_READ_ONLY), false),
                Arguments.of(8, List.of(EVolumeCreateMode.EVolumeCreateMode_READ_ONLY), true),
                Arguments.of(7, List.of(EVolumeCreateMode.EVolumeCreateMode_READ_WRITE), false),
                Arguments.of(7, List.of(), false),
                Arguments.of(
                        7,
                        List.of(
                                EVolumeCreateMode.EVolumeCreateMode_READ_WRITE,
                                EVolumeCreateMode.EVolumeCreateMode_READ_ONLY
                        ),
                        true
                ),
                Arguments.of(
                        7,
                        List.of(
                                EVolumeCreateMode.EVolumeCreateMode_READ_ONLY,
                                EVolumeCreateMode.EVolumeCreateMode_READ_ONLY
                        ),
                        true
                ),
                Arguments.of(
                        7,
                        List.of(
                                EVolumeCreateMode.EVolumeCreateMode_READ_WRITE,
                                EVolumeCreateMode.EVolumeCreateMode_READ_WRITE
                        ),
                        false
                )
        );
    }

    @ParameterizedTest
    @MethodSource("isDeployUnitHasReadOnlyBoxRootfsSource")
    public void isDeployUnitHasReadOnlyBoxRootfsTest(int patcherRevision,
                                                     List<EVolumeCreateMode> volumeCreateModes,
                                                     boolean result) {
        var boxes = volumeCreateModes.stream()
                .map(volumeCreateMode ->
                        TBox.newBuilder()
                                .setRootfs(TRootfsVolume.newBuilder().setCreateMode(volumeCreateMode))
                                .build())
                .collect(Collectors.toList());

        var deployUnitBuilder = TDeployUnitSpec.newBuilder();
        deployUnitBuilder.setPatchersRevision(patcherRevision);
        TestUtils.setUpPodToDeployUnitBuilder(deployUnitBuilder);
        deployUnitBuilder.getReplicaSetBuilder()
                .getReplicaSetTemplateBuilder()
                .getPodTemplateSpecBuilder()
                .getSpecBuilder()
                .getPodAgentPayloadBuilder()
                .getSpecBuilder()
                .addAllBoxes(boxes);

        Assertions.assertEquals(result,
                Utils.isDeployUnitHasReadOnlyBoxRootfs(deployUnitBuilder.build()));
    }

    public static Stream<Arguments> isDeployUnitUseNonPersistentVolumeSource() {
        return Stream.of(
                Arguments.of(List.of(), false),
                Arguments.of(List.of(EVolumePersistenceType.EVolumePersistenceType_NON_PERSISTENT), true),
                Arguments.of(List.of(EVolumePersistenceType.EVolumePersistenceType_PERSISTENT), false),
                Arguments.of(
                        List.of(
                                EVolumePersistenceType.EVolumePersistenceType_NON_PERSISTENT,
                                EVolumePersistenceType.EVolumePersistenceType_NON_PERSISTENT
                        ),
                        true
                ),
                Arguments.of(
                        List.of(
                                EVolumePersistenceType.EVolumePersistenceType_PERSISTENT,
                                EVolumePersistenceType.EVolumePersistenceType_NON_PERSISTENT
                        ),
                        true
                ),
                Arguments.of(
                        List.of(
                                EVolumePersistenceType.EVolumePersistenceType_PERSISTENT,
                                EVolumePersistenceType.EVolumePersistenceType_PERSISTENT
                        ),
                        false
                )
        );
    }

    @ParameterizedTest
    @MethodSource("isDeployUnitUseNonPersistentVolumeSource")
    public void isDeployUnitUseNonPersistentVolumeTest(List<EVolumePersistenceType> volumePersistenceTypes,
                                                       boolean result) {
        var deployUnitBuilder = TDeployUnitSpec.newBuilder();
        TestUtils.setUpPodToDeployUnitBuilder(deployUnitBuilder);
        deployUnitBuilder.getReplicaSetBuilder()
                .getReplicaSetTemplateBuilder()
                .getPodTemplateSpecBuilder()
                .getSpecBuilder()
                .getPodAgentPayloadBuilder()
                .getSpecBuilder()
                .addAllVolumes(
                        volumePersistenceTypes.stream()
                                .map(volumePersistenceType ->
                                        TVolume.newBuilder()
                                                .setPersistenceType(volumePersistenceType)
                                                .build())
                                .collect(Collectors.toList())
                );

        Assertions.assertEquals(result,
                Utils.isDeployUnitUseNonPersistentVolume(deployUnitBuilder.build()));
    }


    public static Stream<Arguments> isDeployUnitUseLayersWithoutDeletingSource() {
        return Stream.of(
                Arguments.of(List.of(), ELayerSourceFileStoragePolicy.ELayerSourceFileStoragePolicy_NONE, false),
                Arguments.of(List.of(), ELayerSourceFileStoragePolicy.ELayerSourceFileStoragePolicy_REMOVE, false),
                Arguments.of(List.of(), ELayerSourceFileStoragePolicy.ELayerSourceFileStoragePolicy_KEEP, true),
                Arguments.of(
                        List.of(ELayerSourceFileStoragePolicy.ELayerSourceFileStoragePolicy_KEEP),
                        ELayerSourceFileStoragePolicy.ELayerSourceFileStoragePolicy_NONE, true
                ),
                Arguments.of(
                        List.of(ELayerSourceFileStoragePolicy.ELayerSourceFileStoragePolicy_NONE),
                        ELayerSourceFileStoragePolicy.ELayerSourceFileStoragePolicy_NONE, false
                ),
                Arguments.of(
                        List.of(
                                ELayerSourceFileStoragePolicy.ELayerSourceFileStoragePolicy_NONE,
                                ELayerSourceFileStoragePolicy.ELayerSourceFileStoragePolicy_KEEP
                        ),
                        ELayerSourceFileStoragePolicy.ELayerSourceFileStoragePolicy_NONE, true
                )
        );
    }

    @ParameterizedTest
    @MethodSource("isDeployUnitUseLayersWithoutDeletingSource")
    public void isDeployUnitUseLayersWithoutDeletingTest(
            List<ELayerSourceFileStoragePolicy> layerSourceFileStoragePolicies,
            ELayerSourceFileStoragePolicy defaultLayerSourceFileStoragePolicy,
            boolean result) {
        var deployUnitBuilder = TDeployUnitSpec.newBuilder();
        TestUtils.setUpPodToDeployUnitBuilder(deployUnitBuilder);
        deployUnitBuilder.getReplicaSetBuilder()
                .getReplicaSetTemplateBuilder()
                .getPodTemplateSpecBuilder()
                .getSpecBuilder()
                .getPodAgentPayloadBuilder()
                .getSpecBuilder()
                .getResourcesBuilder()
                .addAllLayers(
                        layerSourceFileStoragePolicies.stream()
                                .map(layerSourceFileStoragePolicy ->
                                        TLayer.newBuilder()
                                                .setLayerSourceFileStoragePolicy(layerSourceFileStoragePolicy)
                                                .build())
                                .collect(Collectors.toList())
                )
                .setDefaultLayerSourceFileStoragePolicy(defaultLayerSourceFileStoragePolicy);

        Assertions.assertEquals(result,
                Utils.isDeployUnitUseLayersWithoutDeleting(deployUnitBuilder.build()));
    }


    public static Stream<Arguments> isDeployUnitHasOnlySecretsWithSafeAccessPermissionsSource() {
        return Stream.of(
                Arguments.of(List.of(), false),
                Arguments.of(List.of(new TestUtils.ResourceWithSecret(EResourceAccessPermissions_660, true)), true),
                Arguments.of(List.of(new TestUtils.ResourceWithSecret(EResourceAccessPermissions_600, true)), true),
                Arguments.of(List.of(new TestUtils.ResourceWithSecret(EResourceAccessPermissions_UNMODIFIED, true)),
                        false),
                Arguments.of(List.of(new TestUtils.ResourceWithSecret(EResourceAccessPermissions_660, false)), false),
                Arguments.of(List.of(
                        new TestUtils.ResourceWithSecret(EResourceAccessPermissions_660, true),
                        new TestUtils.ResourceWithSecret(EResourceAccessPermissions_600, false)
                ), true),
                Arguments.of(List.of(
                        new TestUtils.ResourceWithSecret(EResourceAccessPermissions_UNMODIFIED, true),
                        new TestUtils.ResourceWithSecret(EResourceAccessPermissions_600, false)
                ), false),
                Arguments.of(List.of(
                        new TestUtils.ResourceWithSecret(EResourceAccessPermissions_UNMODIFIED, false),
                        new TestUtils.ResourceWithSecret(EResourceAccessPermissions_600, true)
                ), true)
        );
    }

    @ParameterizedTest
    @MethodSource("isDeployUnitHasOnlySecretsWithSafeAccessPermissionsSource")
    public void isDeployUnitHasOnlySecretsWithSafeAccessPermissionsTest(
            List<TestUtils.ResourceWithSecret> staticResourceAccessPermissions,
            boolean result) {
        var deployUnitBuilder = TDeployUnitSpec.newBuilder();
        TestUtils.setUpPodToDeployUnitBuilder(deployUnitBuilder);
        deployUnitBuilder.getReplicaSetBuilder()
                .getReplicaSetTemplateBuilder()
                .getPodTemplateSpecBuilder()
                .getSpecBuilder()
                .getPodAgentPayloadBuilder()
                .getSpecBuilder()
                .getResourcesBuilder()
                .addAllStaticResources(
                        staticResourceAccessPermissions.stream()
                                .map(resourceWithSecret -> {
                                    TResource.Builder resource = TResource.newBuilder()
                                            .setAccessPermissions(resourceWithSecret.accessPermissions);
                                    if (resourceWithSecret.hasSecret) {
                                        resource.setFiles(
                                                TFiles.newBuilder()
                                                        .addAllFiles(
                                                                List.of(
                                                                        TFile.newBuilder()
                                                                                .setSecretData(
                                                                                        SecretSelector.getDefaultInstance()
                                                                                ).build()
                                                                )
                                                        ));
                                    }
                                    return resource.build();
                                }).collect(Collectors.toList())
                );

        var stage = createStageWithSox(Map.of(DEFAULT_DEPLOY_UNIT_ID, deployUnitBuilder.build()), true);
        Assertions.assertEquals(result,
                Utils.isDeployUnitHasOnlySecretsWithSafeAccessPermissions(new TStageAndDuId(stage, stage.getSpec()
                        .getDeployUnitsMap().keySet().stream().findFirst().get())));
    }

    public static Stream<Arguments> isDeployUnitUseSequentialDeployingModeSource() {
        return Stream.of(
                Arguments.of(TDeployUnitSpec.TDeploySettings.EDeployStrategy.SEQUENTIAL, true),
                Arguments.of(TDeployUnitSpec.TDeploySettings.EDeployStrategy.PARALLEL, false)
        );
    }

    @ParameterizedTest
    @MethodSource("isDeployUnitUseSequentialDeployingModeSource")
    public void isDeployUnitUseSequentialDeployingModeTest(
            TDeployUnitSpec.TDeploySettings.EDeployStrategy deployStrategy,
            boolean result) {
        var deployUnitBuilder =
                TDeployUnitSpec.newBuilder()
                        .setDeploySettings(
                                TDeployUnitSpec.TDeploySettings.newBuilder()
                                        .setDeployStrategy(deployStrategy)
                        );
        Assertions.assertEquals(result, Utils.isDeployUnitUseSequentialDeployingMode(deployUnitBuilder.build()));
    }

    public static Stream<Arguments> isDeployUnitUseParallelDeployingModeSource() {
        return Stream.of(
                Arguments.of(TDeployUnitSpec.TDeploySettings.EDeployStrategy.SEQUENTIAL, false),
                Arguments.of(TDeployUnitSpec.TDeploySettings.EDeployStrategy.PARALLEL, true)
        );
    }

    @ParameterizedTest
    @MethodSource("isDeployUnitUseParallelDeployingModeSource")
    public void isDeployUnitUseParallelDeployingModeTest(
            TDeployUnitSpec.TDeploySettings.EDeployStrategy deployStrategy,
            boolean result) {
        var deployUnitBuilder =
                TDeployUnitSpec.newBuilder()
                        .setDeploySettings(
                                TDeployUnitSpec.TDeploySettings.newBuilder()
                                        .setDeployStrategy(deployStrategy)
                        );
        Assertions.assertEquals(result, Utils.isDeployUnitUseParallelDeployingMode(deployUnitBuilder.build()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void isDeployUnitUseDefaultDeployingModeTest(boolean isSetUpDeploySettings) {
        var deployUnitBuilder = TDeployUnitSpec.newBuilder();
        if (isSetUpDeploySettings) {
            deployUnitBuilder.setDeploySettings(TDeployUnitSpec.TDeploySettings.newBuilder());
        }
        Assertions.assertEquals(!isSetUpDeploySettings,
                Utils.isDeployUnitUseDefaultDeployingMode(deployUnitBuilder.build()));
    }

    public static Stream<Arguments> getThreadAmountsInBoxesSource() {
        return Stream.of(
                Arguments.of(List.of()),
                Arguments.of(List.of(0)),
                Arguments.of(List.of(1)),
                Arguments.of(List.of(1, 2)),
                Arguments.of(List.of(10, 12, 4)),
                Arguments.of(List.of(1, 5, 6, 3))
        );
    }

    @ParameterizedTest
    @MethodSource("getThreadAmountsInBoxesSource")
    public void getThreadAmountsInBoxesTest(List<Integer> threadInBoxAmounts) {
        var deployUnitBuilder = TDeployUnitSpec.newBuilder();
        TestUtils.setUpPodToDeployUnitBuilder(deployUnitBuilder);
        deployUnitBuilder.getReplicaSetBuilder()
                .getReplicaSetTemplateBuilder()
                .getPodTemplateSpecBuilder()
                .getSpecBuilder()
                .getPodAgentPayloadBuilder()
                .getSpecBuilder()
                .addAllBoxes(
                        threadInBoxAmounts.stream()
                                .map(threadAmount ->
                                        TBox.newBuilder()
                                                .setComputeResources(
                                                        TComputeResources.newBuilder()
                                                                .setThreadLimit(threadAmount)
                                                )
                                                .build()
                                ).collect(Collectors.toList())
                );

        Assertions.assertEquals(threadInBoxAmounts.stream().filter(i -> i > 0).collect(Collectors.toList()),
                Utils.getNotZeroThreadAmountsInBoxes(deployUnitBuilder.build()));
    }

    public static Stream<Arguments> predicateToIntSource() {
        List<Predicate<Integer>> predicates = List.of(i -> true, i -> false, i -> i > 0);
        List<Integer> intValues = List.of(-1, 0, 1, 2, 5, 10, 100);
        return predicates.stream().flatMap(predicate -> intValues.stream().map(intValue -> Arguments.of(predicate,
                intValue)));
    }

    @ParameterizedTest
    @MethodSource("predicateToIntSource")
    public void predicateToIntTest(Predicate<Integer> predicate, int value) {
        var function = Utils.predicateToInt(predicate);
        Assertions.assertEquals(predicate.test(value) ? 1 : 0, function.apply(value));
    }

    public static Stream<Arguments> sumReduceMetricsSource() {
        return Stream.of(
                Arguments.of(0, List.of(), 0),
                Arguments.of(10, List.of(), 10),
                Arguments.of(1, List.of(1), 2),
                Arguments.of(1, List.of(0), 1),
                Arguments.of(0, List.of(1, 2, 3), 6),
                Arguments.of(0, List.of(0, 0, 0), 0),
                Arguments.of(1, List.of(1, 2, -3), 1),
                Arguments.of(0, List.of(10, 20, 30), 60),
                Arguments.of(15, List.of(3, 8, 15), 41),
                Arguments.of(-10, List.of(4, 3, 1), -2),
                Arguments.of(0, List.of(5, 6, 5), 16)
        );
    }

    @ParameterizedTest
    @MethodSource("sumReduceMetricsSource")
    public void sumReduceMetricsTest(int identity, Collection<Integer> values, int metricResult) {
        Assertions.assertEquals(metricResult, Utils.reduceMetrics(identity, Integer::sum).apply(values));
    }

    public static Stream<Arguments> prodReduceMetricsSource() {
        return Stream.of(
                Arguments.of(1, List.of(), 1),
                Arguments.of(1, List.of(2), 2),
                Arguments.of(1, List.of(1, 2, 3), 6),
                Arguments.of(2, List.of(0, 0, 0), 0),
                Arguments.of(1, List.of(1, 2, -3), -6),
                Arguments.of(0, List.of(10, 20, 30), 0),
                Arguments.of(-10, List.of(4, 3, 1), -120),
                Arguments.of(1, List.of(3, 8, 15), 3 * 8 * 15),
                Arguments.of(-1, List.of(5, 6, 5), -5 * 6 * 5)
        );
    }

    @ParameterizedTest
    @MethodSource("prodReduceMetricsSource")
    public void prodReduceMetricsTest(int identity, Collection<Integer> values, int metricResult) {
        Assertions.assertEquals(metricResult, Utils.reduceMetrics(identity, (a, b) -> a * b).apply(values));
    }

    @ParameterizedTest
    @MethodSource("sumReduceMetricsSource")
    public void countMetricsTest(int identity, Collection<Integer> values, int metricResult) {
        Assertions.assertEquals(metricResult - identity, Utils.countMetrics().apply(values));
    }

    public static Stream<Arguments> countThresholdMetricsSource() {
        return Stream.of(
                Arguments.of(0, List.of(), 0),
                Arguments.of(-1, List.of(), 0),
                Arguments.of(10, List.of(10), 1),
                Arguments.of(10, List.of(9), 0),
                Arguments.of(10, List.of(11), 1),
                Arguments.of(0, List.of(1, 2, 3), 3),
                Arguments.of(10, List.of(5, 9, 4), 0),
                Arguments.of(1, List.of(1, 2, -3), 2),
                Arguments.of(0, List.of(10, 20, 30), 3),
                Arguments.of(-10, List.of(4, 3, 1), 3),
                Arguments.of(1, List.of(3, -8, 15), 2),
                Arguments.of(5, List.of(5, 6, 5), 3),
                Arguments.of(5, List.of(4, 5, 4), 1)
        );
    }

    @ParameterizedTest
    @MethodSource("countThresholdMetricsSource")
    public void countThresholdMetricsTest(int threshold, Collection<Integer> values, int metricResult) {
        Assertions.assertEquals(metricResult, Utils.countThresholdMetrics(threshold).apply(values));
    }

    public static Stream<Arguments> orMetricsSource() {
        return Stream.of(
                Arguments.of(List.of(), 0),
                Arguments.of(List.of(0), 0),
                Arguments.of(List.of(1), 1),
                Arguments.of(List.of(0, 0), 0),
                Arguments.of(List.of(0, 1), 1),
                Arguments.of(List.of(1, 0), 1),
                Arguments.of(List.of(1, 1), 1),
                Arguments.of(List.of(0, 0, 0), 0),
                Arguments.of(List.of(0, 0, 1), 1),
                Arguments.of(List.of(0, 1, 0), 1),
                Arguments.of(List.of(0, 1, 1), 1),
                Arguments.of(List.of(1, 0, 0), 1),
                Arguments.of(List.of(1, 1, 1), 1),
                Arguments.of(List.of(0, 0, 0, 0), 0),
                Arguments.of(List.of(0, 1, 1, 0), 1)
        );
    }

    @ParameterizedTest
    @MethodSource("orMetricsSource")
    public void orMetricsTest(Collection<Integer> values, int metricResult) {
        Assertions.assertEquals(metricResult, Utils.orMetrics().apply(values));
    }
}

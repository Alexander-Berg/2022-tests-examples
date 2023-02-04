package ru.yandex.infra.sidecars_updater;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;

import ru.yandex.bolts.function.Function;
import ru.yandex.infra.controller.dto.StageMeta;
import ru.yandex.infra.controller.yp.YpObject;
import ru.yandex.infra.sidecars_updater.util.TStageAndDuId;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.yp.client.api.DataModel;
import ru.yandex.yp.client.api.TDeployUnitSpec;
import ru.yandex.yp.client.api.TDockerImageDescription;
import ru.yandex.yp.client.api.THostInfraInfo;
import ru.yandex.yp.client.api.TLogbrokerConfig;
import ru.yandex.yp.client.api.TMonitoringInfo;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.api.TReplicaSetSpec;
import ru.yandex.yp.client.api.TStageSpec;
import ru.yandex.yp.client.api.TStageStatus;
import ru.yandex.yp.client.api.TTvmConfig;
import ru.yandex.yp.client.pods.EResourceAccessPermissions;
import ru.yandex.yp.client.pods.EVolumeMountMode;
import ru.yandex.yp.client.pods.TBox;
import ru.yandex.yp.client.pods.TComputeResources;
import ru.yandex.yp.client.pods.TIoLimit;
import ru.yandex.yp.client.pods.TPodAgentSpec;
import ru.yandex.yp.client.pods.TUtilityContainer;
import ru.yandex.yp.client.pods.TWorkload;

import static java.util.Collections.emptyMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUtils {
    public static final String STAGE_PREFIX = "stage_";
    public static final String DU_PREFIX = "du_";
    public static final String DEFAULT_STAGE_ID = "stage_id";
    public static final String DEFAULT_DEPLOY_UNIT_ID = "deploy_unit_id";
    public static final int DEFAULT_SPEC_TIMESTAMP = 100;
    public static final int DEFAULT_META_TIMESTAMP = 200;
    public static final int DEFAULT_STATUS_TIMESTAMP = 300;

    public static YpObject<StageMeta, TStageSpec, TStageStatus> createStageYpObject(
            TDeployUnitSpec deployUnitSpec, Map<String, YTreeNode> stageLabels
    ) {
        TStageSpec stageSpec = TStageSpec.newBuilder()
                .putDeployUnits(DEFAULT_DEPLOY_UNIT_ID, deployUnitSpec)
                .build();
        StageMeta stageMeta = mock(StageMeta.class);
        when(stageMeta.getId()).thenReturn(DEFAULT_STAGE_ID);
        TStageStatus stageStatus = TStageStatus.newBuilder().build();

        return new YpObject<>(
                stageSpec,
                stageStatus,
                stageMeta,
                emptyMap(),
                stageLabels,
                DEFAULT_SPEC_TIMESTAMP,
                DEFAULT_STATUS_TIMESTAMP,
                DEFAULT_META_TIMESTAMP
        );
    }

    public static Map<String, YpObject<StageMeta, TStageSpec, TStageStatus>> createStageMap(
            List<YpObject<StageMeta, TStageSpec, TStageStatus>> stages) {
        Map<String, YpObject<StageMeta, TStageSpec, TStageStatus>> stageMap = new HashMap<>();
        for (int i = 0; i < stages.size(); i++) {
            stageMap.put(STAGE_PREFIX + i, stages.get(i));
        }
        return stageMap;
    }

    public static YpObject<StageMeta, TStageSpec, TStageStatus> createStage(Map<String, TDeployUnitSpec> deployUnitMap) {
        return createStageWithSox(deployUnitMap, false);
    }

    public static YpObject<StageMeta, TStageSpec, TStageStatus> createStageWithSox(Map<String, TDeployUnitSpec> deployUnitMap, boolean withSox) {
            var stageBuilder = TStageSpec.newBuilder();
            deployUnitMap.forEach(stageBuilder::putDeployUnits);
            stageBuilder.setSoxService(withSox);
            YpObject.Builder<StageMeta, TStageSpec, TStageStatus> ypBuilder = new YpObject.Builder<>();
            ypBuilder.setSpecAndTimestamp(stageBuilder.build(), 0);
            return ypBuilder.build();
    }

    public static YpObject<StageMeta, TStageSpec, TStageStatus> createStage(List<TDeployUnitSpec> deployUnitList) {
        Map<String, TDeployUnitSpec> deployUnitMap = new HashMap<>();
        for (int i = 0; i < deployUnitList.size(); i++) {
            deployUnitMap.put(DU_PREFIX + i, deployUnitList.get(i));
        }
        return createStage(deployUnitMap);
    }

    public static Map<String, YpObject<StageMeta, TStageSpec, TStageStatus>> createStagesWithDUs(List<Integer> deployUnitAmounts) {
        return createStageMap(deployUnitAmounts.stream().map(deployUnitAmount ->
                createStage(createNDeployUnits(deployUnitAmount))).collect(Collectors.toList()));
    }

    public static Map<String, TDeployUnitSpec> createNDeployUnits(int n) {
        Map<String, TDeployUnitSpec> deployUnitMap = new HashMap<>();
        for (int i = 0; i < n; i++) {
            deployUnitMap.put(DU_PREFIX + i, TDeployUnitSpec.newBuilder().build());
        }
        return deployUnitMap;
    }

    public static TDeployUnitSpec.Builder createDefaultDeployUnit() {
        return createDeployUnitBuilder(false, false, false, false, false);
    }

    public static TDeployUnitSpec.Builder createDeployUnitBuilder(boolean isHasTvm, boolean isUseLogs,
                                                                  boolean isHasDocker,
                                                                  boolean isHasProtoMetrics,
                                                                  boolean isHasCustomLogBroker) {
        var deployUnitBuilder = TDeployUnitSpec.newBuilder();
        var logBrokerBuilder = TLogbrokerConfig.newBuilder();
        if (isHasTvm) {
            deployUnitBuilder.setTvmConfig(TTvmConfig.newBuilder().build());
        }
        if (isUseLogs) {
            logBrokerBuilder.setSidecarBringupMode(TLogbrokerConfig.ESidecarBringupMode.MANDATORY);
        }
        if (isHasDocker) {
            deployUnitBuilder.putImagesForBoxes("docker", TDockerImageDescription.newBuilder().build());
        }
        if (isHasProtoMetrics) {
            deployUnitBuilder.setCollectPortometricsFromSidecars(true);
        }
        if (isHasCustomLogBroker) {
            logBrokerBuilder.setCustomTopicRequest(TLogbrokerConfig.TLogbrokerCustomTopicRequest.newBuilder().build());
        }
        deployUnitBuilder.setLogbrokerConfig(logBrokerBuilder);
        return deployUnitBuilder;
    }

    public static TDeployUnitSpec.Builder addBoxesToDeployUnitBuilder(TDeployUnitSpec.Builder deployUnitBuilder,
                                                                      int boxAmount) {
        setUpPodToDeployUnitBuilder(deployUnitBuilder);
        var podAgentSpecBuilder = deployUnitBuilder.getReplicaSetBuilder().getReplicaSetTemplateBuilder()
                .getPodTemplateSpecBuilder().getSpecBuilder().getPodAgentPayloadBuilder().getSpecBuilder();
        for (int i = 0; i < boxAmount; i++) {
            podAgentSpecBuilder.addBoxes(TBox.newBuilder());
        }
        return deployUnitBuilder;
    }

    public static void setUpPodToDeployUnitBuilder(TDeployUnitSpec.Builder deployUnitBuilder) {
        var replicaSetDeployBuilder = TDeployUnitSpec.TReplicaSetDeploy.newBuilder();
        var replicaSetBuilder = TReplicaSetSpec.newBuilder();
        var podBuilder = TPodTemplateSpec.newBuilder();
        podBuilder.setSpec(DataModel.TPodSpec.newBuilder());
        replicaSetBuilder.setPodTemplateSpec(podBuilder);
        replicaSetDeployBuilder.setReplicaSetTemplate(replicaSetBuilder);
        deployUnitBuilder.setReplicaSet(replicaSetDeployBuilder);
    }

    public static void setUpMonitoringToDeployUnitBuilder(TDeployUnitSpec.Builder deployUnitBuilder) {
        var podBuilder = getPodBuilder(deployUnitBuilder);
        var hostInfraBuilder = THostInfraInfo.newBuilder();
        hostInfraBuilder.setMonitoring(TMonitoringInfo.newBuilder());
        podBuilder.setHostInfra(hostInfraBuilder);
    }

    public static void setWorkloadsToPodAgent(TDeployUnitSpec.Builder deployUnitBuilder,
                                              List<TWorkload.Builder> workloadBuilders) {
        var podBuilder = getPodBuilder(deployUnitBuilder);
        var podAgentPayloadBuilder = DataModel.TPodSpec.TPodAgentPayload.newBuilder();
        var podAgentSpec = TPodAgentSpec.newBuilder();
        workloadBuilders.forEach(podAgentSpec::addWorkloads);
        podAgentPayloadBuilder.setSpec(podAgentSpec);
        podBuilder.setPodAgentPayload(podAgentPayloadBuilder);
    }

    public static void setWorkloadsByIdsToPodAgent(TDeployUnitSpec.Builder deployUnitBuilder,
                                                   List<String> workloadIds) {
        List<TWorkload.Builder> workloadBuilders = workloadIds.stream()
                .map(id -> TWorkload.newBuilder().setId(id))
                .collect(Collectors.toList());

        setWorkloadsToPodAgent(deployUnitBuilder, workloadBuilders);
    }

    public static TWorkload.Builder setWorkloadsLimits(TWorkload.Builder workloadBuilder, Limits workloadLimits) {
        var utilityContainerBuilder = TUtilityContainer.newBuilder();
        utilityContainerBuilder.setComputeResources(getComputeResourcesWithLimits(workloadLimits));
        workloadBuilder.setStart(utilityContainerBuilder);
        return workloadBuilder;
    }

    public static TComputeResources.Builder getComputeResourcesWithLimits(Limits limits) {
        var computeResourcesBuilder = TComputeResources.newBuilder();
        computeResourcesBuilder.setVcpuLimit(limits.vcuLimit);
        computeResourcesBuilder.setMemoryLimit(limits.memoryLimit);
        computeResourcesBuilder.setAnonymousMemoryLimit(limits.anonymousMemoryLimit);
        if (limits.isTIoLimit) {
            computeResourcesBuilder.addIoLimit(TIoLimit.newBuilder());
        }
        if (limits.isTIoOpsLimit) {
            computeResourcesBuilder.addIoOpsLimit(TIoLimit.newBuilder());
        }
        return computeResourcesBuilder;
    }

    public static DataModel.TPodSpec.TResourceRequests.Builder getResourceRequest(Guarantees guarantees) {
        var resourceRequestBuilder = DataModel.TPodSpec.TResourceRequests.newBuilder();
        resourceRequestBuilder.setVcpuGuarantee(guarantees.vcuGuarantee);
        resourceRequestBuilder.setMemoryLimit(guarantees.memoryGuarantee);
        resourceRequestBuilder.setNetworkBandwidthGuarantee(guarantees.networkBandwidthGuarantee);
        return resourceRequestBuilder;
    }

    public static DataModel.TPodSpec.Builder getPodBuilder(TDeployUnitSpec.Builder deployUnitBuilder) {
        return deployUnitBuilder.getReplicaSetBuilder().getReplicaSetTemplateBuilder()
                .getPodTemplateSpecBuilder().getSpecBuilder();
    }

    public static Map<String, YpObject<StageMeta, TStageSpec, TStageStatus>> generateStagesByIds(
            Map<Collection<Integer>, Integer> duIdsToStageId,
            Map<TStageAndDuId, Integer> duToId) {
        return TestUtils.createStageMap(
                duIdsToStageId.keySet().stream()
                        .map(duIds -> {
                                    YpObject<StageMeta, TStageSpec, TStageStatus> stage = TestUtils.createStage(
                                            duIds.stream()
                                                    .map(duId -> {
                                                        TDeployUnitSpec deployUnit = TDeployUnitSpec.newBuilder()
                                                                .setRevision(duId)
                                                                .build();
                                                        return deployUnit;
                                                    })
                                                    .collect(Collectors.toList()));
                                    stage.getSpec().getDeployUnitsMap().forEach((id, spec) -> duToId.put(new TStageAndDuId(stage, id), spec.getRevision()));
                                    return stage;
                                }
                        ).collect(Collectors.toList())
        );
    }

    public static <T> Map<String, YpObject<StageMeta, TStageSpec, TStageStatus>> generateStagesByNamesAndDUValues(
            Map<String, Map<String, T>> stageNameToDUNameToDUValue,
            Map<TStageAndDuId, T> duToValue,
            Map<Collection<T>, T> duValuesToStageValue,
            T identity,
            BinaryOperator<T> sum) {
        AtomicInteger curDUId = new AtomicInteger();
        return stageNameToDUNameToDUValue.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                stageEntry -> {
                    Set<T> duValuesForCurStage = new HashSet<>();
                    HashMap<Integer, T> valueByRev = new HashMap<>();
                    YpObject<StageMeta, TStageSpec, TStageStatus> stage =
                            TestUtils.createStage(stageEntry.getValue().entrySet().stream().collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    duEntry -> {
                                        var deployUnit =
                                                TDeployUnitSpec.newBuilder().setRevision(curDUId.getAndIncrement()).build();
                                        valueByRev.put(deployUnit.getRevision(), duEntry.getValue());
                                        duValuesForCurStage.add(duEntry.getValue());
                                        return deployUnit;
                                    }
                            )));
                    stage.getSpec().getDeployUnitsMap().forEach((id, spec) -> duToValue.put(new TStageAndDuId(stage, id), valueByRev.get(spec.getRevision())));
                    duValuesToStageValue.put(duValuesForCurStage, duValuesForCurStage.stream().reduce(identity, sum));
                    return stage;
                }
        ));

    }


    public static class Limits {
        public final long vcuLimit;
        public final long memoryLimit;
        public final long anonymousMemoryLimit;
        public final boolean isTIoLimit;
        public final boolean isTIoOpsLimit;

        public Limits(long vcuLimit, long memoryLimit, long anonymousMemoryLimit, boolean isTIoLimit,
                      boolean isTIoOpsLimit) {
            this.vcuLimit = vcuLimit;
            this.memoryLimit = memoryLimit;
            this.anonymousMemoryLimit = anonymousMemoryLimit;
            this.isTIoLimit = isTIoLimit;
            this.isTIoOpsLimit = isTIoOpsLimit;
        }
    }

    public static class Guarantees {
        public long vcuGuarantee;
        public long memoryGuarantee;
        public long networkBandwidthGuarantee;

        public Guarantees(long vcuGuarantee, long memoryGuarantee, long networkBandwidthGuarantee) {
            this.vcuGuarantee = vcuGuarantee;
            this.memoryGuarantee = memoryGuarantee;
            this.networkBandwidthGuarantee = networkBandwidthGuarantee;
        }
    }

    public static class DiskQuotaPolicy {
        public long ioLimit;
        public long ioGuarantee;

        public DiskQuotaPolicy(long ioLimit, long ioGuarantee) {
            this.ioLimit = ioLimit;
            this.ioGuarantee = ioGuarantee;
        }
    }

    public static class DummyMetric<K, V> implements Function<K, V> {
        protected Map<K, V> metricResultMap;
        protected Set<K> usedKeys;

        public DummyMetric(Map<K, V> metricResultMap) {
            this.metricResultMap = metricResultMap;
            usedKeys = new HashSet<>();
        }

        @Override
        public V apply(K key) {
            V result = metricResultMap.get(key);
            if (result == null || !usedKeys.add(key)) {
                throw new RuntimeException();
            }
            return result;
        }

        public void checkAllKeysUsed() {
            Assertions.assertEquals(metricResultMap.size(), usedKeys.size());
        }
    }

    public static class SetDummyMetric<K, V> extends DummyMetric<Collection<K>, V> {
        public SetDummyMetric(Map<Collection<K>, V> metricResultMap) {
            super(metricResultMap);
        }

        public V apply(Collection<K> key) {
            return super.apply(new HashSet<>(key));
        }
    }

    public static class Volume {
        public String id;
        public int staticResourcesCount;
        public int layerRefsCount;

        public Volume(String id, int staticResourcesCount, int layerRefsCount) {
            this.id = id;
            this.staticResourcesCount = staticResourcesCount;
            this.layerRefsCount = layerRefsCount;
        }
    }

    public static class MountedVolume {
        public String ref;
        public EVolumeMountMode mountMode;

        public MountedVolume(String ref, EVolumeMountMode mountMode) {
            this.ref = ref;
            this.mountMode = mountMode;
        }
    }

    public static class ResourceWithSecret {
        public EResourceAccessPermissions accessPermissions;
        public boolean hasSecret;

        public ResourceWithSecret(EResourceAccessPermissions accessPermissions, boolean hasSecret) {
            this.accessPermissions = accessPermissions;
            this.hasSecret = hasSecret;
        }
    }
}

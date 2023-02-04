package ru.yandex.infra.stage.podspecs.patcher.coredump;

import java.util.Optional;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.dto.CoredumpConfig;
import ru.yandex.infra.stage.dto.CoredumpOutputPolicy;
import ru.yandex.infra.stage.dto.DownloadableResource;
import ru.yandex.infra.stage.podspecs.FixedResourceSupplier;
import ru.yandex.infra.stage.podspecs.patcher.PatcherTestBase;
import ru.yandex.yp.client.api.DataModel;
import ru.yandex.yp.client.api.TMonitoringWorkloadEndpoint;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.pods.TBox;
import ru.yandex.yp.client.pods.TLayer;
import ru.yandex.yp.client.pods.TPodAgentSpec;
import ru.yandex.yp.client.pods.TResource;
import ru.yandex.yp.client.pods.TUlimitSoft;
import ru.yandex.yp.client.pods.TWorkload;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static ru.yandex.infra.stage.TestData.DEFAULT_BOX_ID;
import static ru.yandex.infra.stage.TestData.DEFAULT_ITYPE;
import static ru.yandex.infra.stage.TestData.DEFAULT_STAGE_CONTEXT;
import static ru.yandex.infra.stage.TestData.DEFAULT_STAGE_ID;
import static ru.yandex.infra.stage.TestData.DEFAULT_UNIT_CONTEXT;
import static ru.yandex.infra.stage.TestData.DEFAULT_WORKLOAD_ID;
import static ru.yandex.infra.stage.TestData.DEFAULT_WORKLOAD_ITYPE;
import static ru.yandex.infra.stage.podspecs.PatcherTestUtils.groupById;
import static ru.yandex.infra.stage.podspecs.PodSpecUtils.MEGABYTE;
import static ru.yandex.infra.stage.podspecs.patcher.coredump.CoredumpPatcherV1Base.COREDUMPS_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.coredump.CoredumpPatcherV1Base.getDefaultItype;
import static ru.yandex.infra.stage.podspecs.patcher.coredump.CoredumpPatcherV1Base.getUlimitSoftCoredumpIndex;
import static ru.yandex.yp.client.pods.EContainerULimitType.EContainerULimit_CORE;
import static ru.yandex.yp.client.pods.EContainerULimitType.EContainerULimit_FSIZE;
import static ru.yandex.yp.client.pods.EContainerULimitType.EContainerULimit_NOFILE;

abstract class CoredumpPatcherV1BaseTest extends PatcherTestBase<CoredumpPatcherV1Context> {
    protected static final long RELEASE_GETTER_TIMEOUT_SECONDS = 1;
    protected static final DownloadableResource INSTANCECTL_DEFAULT = TestData.DOWNLOADABLE_RESOURCE;
    protected static final DownloadableResource GDB_DEFAULT = TestData.DOWNLOADABLE_RESOURCE2;

    private static void addVolumeToBox(DataModel.TPodSpec.TPodAgentPayload.Builder builder, String boxId,
                                       String volumeId, String volumeMountPoint) {
        var box = builder.getSpecBuilder().getBoxesBuilderList().stream()
                .filter(b -> b.getId().equals(boxId))
                .findFirst()
                .orElseThrow();

        TestData.addVolumeToBox(box, volumeId, volumeMountPoint);
    }

    private static void addBox(DataModel.TPodSpec.TPodAgentPayload.Builder builder, String boxId) {
        builder.getSpecBuilder().addBoxes(TestData.createBox(boxId));
    }

    private static void addBox(DataModel.TPodSpec.TPodAgentPayload.Builder builder, String boxId, String allocationId) {
        var box = TBox.newBuilder().setId(boxId).setVirtualDiskIdRef(allocationId);
        builder.getSpecBuilder().addBoxes(box);
    }

    private static void addWorkload(DataModel.TPodSpec.TPodAgentPayload.Builder builder, String workloadId,
                                    String boxId) {
        var workload = TestData.createWorkloadWithLogs(workloadId, boxId);
        builder.getSpecBuilder().addWorkloads(workload);
    }

    private static void addWorkload(DataModel.TPodSpec.TPodAgentPayload.Builder builder, String workloadId,
                                    String boxId, long nofileLimit, long fsizeLimit) {
        var workload = TWorkload.newBuilder()
                .setId(workloadId)
                .setBoxRef(boxId)
                .addUlimitSoft(TUlimitSoft.newBuilder()
                        .setName(EContainerULimit_NOFILE)
                        .setValue(nofileLimit)
                )
                .addUlimitSoft(TUlimitSoft.newBuilder()
                        .setName(EContainerULimit_FSIZE)
                        .setValue(fsizeLimit)
                )
                .addUlimitSoft(TUlimitSoft.newBuilder()
                        .setName(EContainerULimit_CORE)
                        .setValue(1)
                );
        builder.getSpecBuilder().addWorkloads(workload);
    }

    protected static TPodTemplateSpec.Builder createTemplateSpec(String boxId, String workloadId) {
        var templateSpec = createPodSpecBuilder();

        var agentPayloadBuilder = templateSpec
                .getSpecBuilder()
                .getPodAgentPayloadBuilder();

        addBox(agentPayloadBuilder, boxId);
        addWorkload(agentPayloadBuilder, workloadId, boxId);

        return templateSpec;
    }

    private static TWorkload getPatchedWorkload(TPodAgentSpec templateSpec, String workloadId) {
        return templateSpec.getWorkloadsList().stream()
                .filter(w -> w.getId().equals(workloadId))
                .findFirst()
                .orElseThrow();
    }

    static Stream<Arguments> checkNewItypeSource() {
        return Stream.of(
                Arguments.of(Optional.empty(), Optional.empty(), TestData.COREDUMP_CONFIG_WITH_AGGREGATION_WITHOUT_SERVICE),
                Arguments.of(Optional.empty(), Optional.empty(), TestData.COREDUMP_CONFIG_WITH_AGGREGATION),
                Arguments.of(Optional.empty(), Optional.of(DEFAULT_ITYPE), TestData.COREDUMP_CONFIG_WITH_AGGREGATION_WITHOUT_SERVICE),
                Arguments.of(Optional.empty(), Optional.of(DEFAULT_ITYPE), TestData.COREDUMP_CONFIG_WITH_AGGREGATION),
                Arguments.of(Optional.of(DEFAULT_WORKLOAD_ITYPE), Optional.empty(), TestData.COREDUMP_CONFIG_WITH_AGGREGATION_WITHOUT_SERVICE),
                Arguments.of(Optional.of(DEFAULT_WORKLOAD_ITYPE), Optional.empty(), TestData.COREDUMP_CONFIG_WITH_AGGREGATION),
                Arguments.of(Optional.of(DEFAULT_WORKLOAD_ITYPE), Optional.of(DEFAULT_ITYPE), TestData.COREDUMP_CONFIG_WITH_AGGREGATION_WITHOUT_SERVICE),
                Arguments.of(Optional.of(DEFAULT_WORKLOAD_ITYPE), Optional.of(DEFAULT_ITYPE), TestData.COREDUMP_CONFIG_WITH_AGGREGATION)
        );
    }

    protected CoredumpPatcherV1Context createPatcherContext() {
        return new CoredumpPatcherV1Context(
                FixedResourceSupplier.withMeta(INSTANCECTL_DEFAULT, TestData.RESOURCE_META),
                FixedResourceSupplier.withMeta(GDB_DEFAULT, TestData.RESOURCE_META),
                RELEASE_GETTER_TIMEOUT_SECONDS
        );
    }

    @Test
    void patchCoredumpToolsWithInstancectlAndGDB() {
        String boxId = "box_1";
        String workloadId = "workload_1";
        var templateSpec = createTemplateSpec(boxId, workloadId);
        var context = DEFAULT_UNIT_CONTEXT.withCoredumpConfig(ImmutableMap.of(workloadId,
                TestData.COREDUMP_CONFIG_WITH_AGGREGATION));

        var agentSpec = patch(createPatcherContext(), templateSpec, context).getPodAgentSpec();

        var resourceMap = groupById(agentSpec.getResources().getStaticResourcesList(), TResource::getId);
        assertThat(resourceMap.keySet(), containsInAnyOrder("instancectl-binary_"));

        var layerMap = groupById(agentSpec.getResources().getLayersList(), TLayer::getId);
        assertThat(layerMap.keySet(), containsInAnyOrder("gdb-layer_"));
    }

    @Test
    void patchCoredumpToolsWithInstancectlAndGDBWhenAllocationsUsed() {
        String allocation1 = "allocation_1";
        String boxId1 = "box_1";
        String workloadId1 = "workload_1";

        String allocation2 = "allocation_2";
        String boxId2 = "box_2";
        String workloadId2 = "workload_2";

        var templateSpec = createPodSpecBuilder();

        var agentPayloadBuilder = templateSpec
                .getSpecBuilder()
                .getPodAgentPayloadBuilder();

        addBox(agentPayloadBuilder, boxId1, allocation1);
        addWorkload(agentPayloadBuilder, workloadId1, boxId1);

        addBox(agentPayloadBuilder, boxId2, allocation2);
        addWorkload(agentPayloadBuilder, workloadId2, boxId2);

        var context = DEFAULT_UNIT_CONTEXT.withCoredumpConfig(ImmutableMap.of(workloadId1,
                TestData.COREDUMP_CONFIG_WITH_AGGREGATION, workloadId2,
                TestData.COREDUMP_CONFIG_WITH_AGGREGATION));

        var agentSpec = patch(createPatcherContext(), templateSpec, context).getPodAgentSpec();

        var resourceMap = groupById(agentSpec.getResources().getStaticResourcesList(), TResource::getId);
        assertThat(resourceMap.keySet(), containsInAnyOrder(
                        "instancectl-binary_allocation_1",
                        "instancectl-binary_allocation_2"
                )
        );

        var layerMap = groupById(agentSpec.getResources().getLayersList(), TLayer::getId);
        assertThat(layerMap.keySet(), containsInAnyOrder("gdb-layer_allocation_1", "gdb-layer_allocation_2"));
    }

    @Test
    void patchCoredumpToolsWithInstancectl() {
        String boxId = "box_1";
        String workloadId = "workload_1";

        var templateSpec = createTemplateSpec(boxId, workloadId);
        var context = DEFAULT_UNIT_CONTEXT.withCoredumpConfig(ImmutableMap.of(workloadId,
                TestData.COREDUMP_CONFIG_WITHOUT_AGGREGATION));

        var agentSpec = patch(createPatcherContext(), templateSpec, context).getPodAgentSpec();

        var resourceMap = groupById(agentSpec.getResources().getStaticResourcesList(), TResource::getId);
        assertThat(resourceMap.keySet(), containsInAnyOrder("instancectl-binary_"));

        var layerMap = groupById(agentSpec.getResources().getLayersList(), TLayer::getId);
        assertThat(layerMap.keySet(), not(containsInAnyOrder("gdb-layer_")));
    }

    @Test
    void patchWitchCoredumpConfig() {
        String boxId1 = "box_1";
        String boxId2 = "box_2";
        String workloadWithCoredumpAggregation = "workload_1";
        String workloadWithoutCoredumpAggregation = "workload_2";
        String workloadWithoutCoredump = "workload_3";
        var templateSpec = createPodSpecBuilder();
        var agentPayloadBuilder = templateSpec
                .getSpecBuilder()
                .getPodAgentPayloadBuilder();

        addBox(agentPayloadBuilder, boxId1);
        addBox(agentPayloadBuilder, boxId2);
        addWorkload(agentPayloadBuilder, workloadWithCoredumpAggregation, boxId1, 100500, 500100);
        addWorkload(agentPayloadBuilder, workloadWithoutCoredumpAggregation, boxId1);
        addWorkload(agentPayloadBuilder, workloadWithoutCoredump, boxId2);

        var context = DEFAULT_UNIT_CONTEXT.withCoredumpConfig(ImmutableMap.of(
                workloadWithCoredumpAggregation, TestData.COREDUMP_CONFIG_WITH_AGGREGATION,
                workloadWithoutCoredumpAggregation, TestData.COREDUMP_CONFIG_WITHOUT_AGGREGATION));

        var agentSpec = patch(createPatcherContext(), templateSpec, context).getPodAgentSpec();

        var workloadWithCoredumpAggregationPatched = getPatchedWorkload(agentSpec, workloadWithCoredumpAggregation);
        assertThat(workloadWithCoredumpAggregationPatched.getStart().getCoreCommand(),
                containsString("instancectl core_process"));
        assertThat(workloadWithCoredumpAggregationPatched.getStart().getCoreCommand(),
                containsString("--aggr-url=http"));
        assertThat(workloadWithCoredumpAggregationPatched.getStart().getCoreCommand(),
                containsString(String.format("--output %s/%s_%s", COREDUMPS_PATH, DEFAULT_STAGE_CONTEXT.getStageId(),
                        workloadWithCoredumpAggregation)));

        assertThat(workloadWithCoredumpAggregationPatched.getUlimitSoftCount(), equalTo(3));
        int ulimitSoftCoredumpIndex =
                getUlimitSoftCoredumpIndex(workloadWithCoredumpAggregationPatched.getUlimitSoftList()).get();
        assertThat(workloadWithCoredumpAggregationPatched.getUlimitSoft(ulimitSoftCoredumpIndex).getName(),
                equalTo(EContainerULimit_CORE));
        assertThat(workloadWithCoredumpAggregationPatched.getUlimitSoft(ulimitSoftCoredumpIndex).getValue(),
                equalTo(TestData.COREDUMP_CONFIG_WITH_AGGREGATION.getTotalSizeLimit() * MEGABYTE));

        var workloadWithoutCoredumpAggregationPatched =
                getPatchedWorkload(agentSpec, workloadWithoutCoredumpAggregation);
        assertThat(workloadWithoutCoredumpAggregationPatched.getStart().getCoreCommand(),
                containsString("instancectl core_process"));
        assertThat(workloadWithoutCoredumpAggregationPatched.getStart().getCoreCommand(),
                containsString(String.format("--output %s/%s_%s", COREDUMPS_PATH, DEFAULT_STAGE_CONTEXT.getStageId(),
                        workloadWithoutCoredumpAggregation)));
        assertThat(workloadWithoutCoredumpAggregationPatched.getStart().getCoreCommand(),
                not(containsString("--aggr-url=http")));
        assertThat(workloadWithoutCoredumpAggregationPatched.getUlimitSoftCount(), equalTo(1));
        assertThat(workloadWithoutCoredumpAggregationPatched.getUlimitSoft(0).getName(),
                equalTo(EContainerULimit_CORE));
        assertThat(workloadWithoutCoredumpAggregationPatched.getUlimitSoft(0).getValue(),
                equalTo(TestData.COREDUMP_CONFIG_WITHOUT_AGGREGATION.getTotalSizeLimit() * MEGABYTE));

        var workloadWithoutCoredumpPatched = getPatchedWorkload(agentSpec, workloadWithoutCoredump);
        assertThat(workloadWithoutCoredumpPatched.getStart().getCoreCommand(), equalTo(""));
        assertThat(workloadWithoutCoredumpPatched.getUlimitSoftCount(), equalTo(0));
    }

    @Test
    void patchCoredumpCommandWithOutputPathFromConfig() {
        String boxId = "box";
        String workloadId = "workload";
        String coredumpOutputPath = "coredump_output_path";
        var outPolicy = CoredumpOutputPolicy.outputPath(coredumpOutputPath);

        var templateSpec = createTemplateSpec(boxId, workloadId);
        var context = DEFAULT_UNIT_CONTEXT.withCoredumpConfig(
                ImmutableMap.of(
                        workloadId,
                        TestData.COREDUMP_CONFIG_WITHOUT_AGGREGATION.withOutputPolicy(outPolicy)
                )
        );

        var agentSpec = patch(createPatcherContext(), templateSpec, context).getPodAgentSpec();

        var patchedWorkload = getPatchedWorkload(agentSpec, workloadId);

        assertThat(patchedWorkload.getStart().getCoreCommand(),
                containsString(String.format("--output %s", coredumpOutputPath)));
    }

    @Test
    void patchCoredumpCommandWithOutputVolume() {
        String boxId = "box";
        String workloadId = "workload";
        String volumeMountPoint = "mount_point";
        String volumeId = "volume_id";
        var outPolicy = CoredumpOutputPolicy.outputVolumeId(volumeId);

        var templateSpec = createTemplateSpec(boxId, workloadId);
        addVolumeToBox(templateSpec.getSpecBuilder().getPodAgentPayloadBuilder(), boxId, volumeId, volumeMountPoint);

        var context = DEFAULT_UNIT_CONTEXT.withCoredumpConfig(
                ImmutableMap.of(
                        workloadId,
                        TestData.COREDUMP_CONFIG_WITHOUT_AGGREGATION.withOutputPolicy(outPolicy)
                )
        );

        var agentSpec = patch(createPatcherContext(), templateSpec, context).getPodAgentSpec();

        var patchedWorkload = getPatchedWorkload(agentSpec, workloadId);

        assertThat(patchedWorkload.getStart().getCoreCommand(),
                containsString(String.format("--output %s/%s_%s", volumeMountPoint,
                        DEFAULT_STAGE_CONTEXT.getStageId(), workloadId)));
    }

    @Test
    void checkCoredumpToolsDefaultRevisions() {
        String boxId = "box_1";
        String workloadId = "workload_1";
        var templateSpec = createTemplateSpec(boxId, workloadId);
        var context = DEFAULT_UNIT_CONTEXT
                .withCoredumpConfig(ImmutableMap.of(workloadId, TestData.COREDUMP_CONFIG_WITH_AGGREGATION));

        var resources = patch(createPatcherContext(), templateSpec, context).getPodAgentSpec().getResources();

        var staticResourceMap = groupById(resources.getStaticResourcesList(), TResource::getId);
        var instanceCtlBin = staticResourceMap.get("instancectl-binary_");
        assertThat(instanceCtlBin.getUrl(), equalTo(INSTANCECTL_DEFAULT.getUrl()));

        var layerMap = groupById(resources.getLayersList(), TLayer::getId);
        var gdbLayer = layerMap.get("gdb-layer_");
        assertThat(gdbLayer.getUrl(), equalTo(GDB_DEFAULT.getUrl()));
    }

    @ParameterizedTest
    @MethodSource("checkNewItypeSource")
    void checkNewItypeTest(Optional<String> monitoringWorkloadItype, Optional<String> monitoringItype, CoredumpConfig coredumpConfig) {
        TPodTemplateSpec.Builder templateSpec = createPodSpecBuilder();
        var monitoringBuilder = templateSpec.getSpecBuilder().getHostInfraBuilder().getMonitoringBuilder();
        monitoringWorkloadItype.ifPresent(it -> monitoringBuilder
                .addWorkloads(TMonitoringWorkloadEndpoint.newBuilder()
                        .setWorkloadId(DEFAULT_WORKLOAD_ID)
                        .putLabels("itype", it)
                        .build()));
        monitoringItype.ifPresent(it -> monitoringBuilder
                .putLabels("itype", it));

        var agentPayloadBuilder = templateSpec
                .getSpecBuilder()
                .getPodAgentPayloadBuilder();

        addBox(agentPayloadBuilder, DEFAULT_BOX_ID);
        addWorkload(agentPayloadBuilder, DEFAULT_WORKLOAD_ID, DEFAULT_BOX_ID, 100500, 500100);
        var context = DEFAULT_UNIT_CONTEXT.withCoredumpConfig(ImmutableMap.of(
                DEFAULT_WORKLOAD_ID, coredumpConfig));

        var agentSpec = patch(createPatcherContext(), templateSpec, context).getPodAgentSpec();

        var workloadPatched = getPatchedWorkload(agentSpec, DEFAULT_WORKLOAD_ID);
        assertThat(workloadPatched.getStart().getCoreCommand(),
                containsString(String.format("--svc-name=%s", expectedItype(DEFAULT_STAGE_ID,
                        DEFAULT_WORKLOAD_ID, monitoringWorkloadItype, monitoringItype, coredumpConfig))));
    }

    protected String expectedItype(String stageId, String workloadId, Optional<String> monitoringWorkloadItype, Optional<String> monitoringItype, CoredumpConfig coredumpConfig) {
        return coredumpConfig.getServiceName().orElse(getDefaultItype(stageId, workloadId));
    }
}

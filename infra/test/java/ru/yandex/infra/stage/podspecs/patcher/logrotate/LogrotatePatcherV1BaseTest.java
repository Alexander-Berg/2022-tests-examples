package ru.yandex.infra.stage.podspecs.patcher.logrotate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.deployunit.DeployUnitContext;
import ru.yandex.infra.stage.dto.DeployUnitSpec;
import ru.yandex.infra.stage.dto.LogrotateConfig;
import ru.yandex.infra.stage.podspecs.FixedResourceSupplier;
import ru.yandex.infra.stage.podspecs.ResourceSupplier;
import ru.yandex.infra.stage.podspecs.patcher.PatcherTestBase;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.pods.TBox;
import ru.yandex.yp.client.pods.TMountedVolume;
import ru.yandex.yp.client.pods.TRootfsVolume;
import ru.yandex.yp.client.pods.TUtilityContainer;
import ru.yandex.yp.client.pods.TWorkload;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.infra.stage.podspecs.patcher.logrotate.LogrotatePatcherV1Base.LOG_ROTATE_LOG_MOUNT_POINT;
import static ru.yandex.infra.stage.podspecs.patcher.logrotate.LogrotatePatcherV1Base.POSSIBLE_VAR_LOG_MOUNT_POINTS;

abstract class LogrotatePatcherV1BaseTest extends PatcherTestBase<LogrotatePatcherV1Context> {
    private static final ResourceSupplier SUPPLIER = FixedResourceSupplier.withMeta(TestData.DOWNLOADABLE_RESOURCE,
            TestData.RESOURCE_META);
    public static final String BOX_ID = "Test_box";
    public static final String BOX_ID_2 = "Test_box_2";
    public static final String VOLUME_REF = "VolumeRef";
    public static final String ALLOCATION_ID_1 = "allocation1";
    public static final String ALLOCATION_ID_2 = "allocation2";
    private static final LogrotatePatcherV1Context DEFAULT_PATCHER_CONTEXT = new LogrotatePatcherV1Context(SUPPLIER);

    @Test
    public void extendPodSpec() {
        TPodTemplateSpec.Builder builder = TPodTemplateSpec.newBuilder();

        TBox.Builder boxBuilder = TBox.newBuilder();

        builder.getSpecBuilder().getPodAgentPayloadBuilder().getSpecBuilder().addBoxes(boxBuilder
                .setRootfs(TRootfsVolume.newBuilder()
                        .addLayerRefs("some-layer"))
                .addVolumes(TMountedVolume.newBuilder().setVolumeRef(VOLUME_REF)).setId(BOX_ID));

        DeployUnitContext deployUnitContext = mock(DeployUnitContext.class);

        Map<String, LogrotateConfig> config = Map.of(BOX_ID, new LogrotateConfig("Config", 0));
        DeployUnitSpec deployUnitSpec = mock(DeployUnitSpec.class);
        when(deployUnitSpec.getLogrotateConfig()).thenReturn(config);
        when(deployUnitContext.getSpec()).thenReturn(deployUnitSpec);
        var podAgentSpec = patch(DEFAULT_PATCHER_CONTEXT, builder, deployUnitContext).getPodAgentSpec();

        List<TBox> boxesList = podAgentSpec.getBoxesList();
        assertThat(boxesList.isEmpty(), equalTo(false));

        TBox tBox = boxesList.get(0);

        assertThat(tBox.getStaticResourcesList(), not(empty()));
        assertThat(tBox.getRootfs().getLayerRefsList(), not(empty()));

        assertThat(tBox.getRootfs().getLayerRefs(0).contains(LogrotatePatcherV1Base.makeId(boxBuilder,
                LogrotatePatcherV1Base.LOG_ROTATE_APP_LAYER_ID)), equalTo(true));

        assertThat(tBox.getStaticResources(0).getResourceRef()
                , equalTo(LogrotatePatcherV1Base.makeId(boxBuilder,
                        LogrotatePatcherV1Base.LOG_ROTATE_CONFIG_RESOURCE_ID)));
    }

    @Test
    public void patchSpecWithTwoBox() {
        TPodTemplateSpec.Builder builder = TPodTemplateSpec.newBuilder();

        TBox box1 = TBox.newBuilder()
                .addVolumes(TMountedVolume.newBuilder().setVolumeRef(VOLUME_REF)).setVirtualDiskIdRef(ALLOCATION_ID_1).setId(BOX_ID).build();

        TBox box2 = TBox.newBuilder()
                .addVolumes(TMountedVolume.newBuilder().setVolumeRef(VOLUME_REF)).setVirtualDiskIdRef(ALLOCATION_ID_1).setId(BOX_ID_2).build();

        builder.getSpecBuilder().getPodAgentPayloadBuilder().getSpecBuilder().addBoxes(box1).addBoxes(box2);


        DeployUnitContext deployUnitContext = mock(DeployUnitContext.class);

        Map<String, LogrotateConfig> config = Map.of(BOX_ID, new LogrotateConfig("Config", 0), BOX_ID_2,
                new LogrotateConfig("Config", 0));

        DeployUnitSpec deployUnitSpec = mock(DeployUnitSpec.class);
        when(deployUnitSpec.getLogrotateConfig()).thenReturn(config);
        when(deployUnitContext.getSpec()).thenReturn(deployUnitSpec);
        var podAgentSpec = patch(DEFAULT_PATCHER_CONTEXT, builder, deployUnitContext).getPodAgentSpec();

        List<TBox> boxesList = podAgentSpec.getBoxesList();
        assertThat(boxesList.size(), equalTo(2));

        TBox tBox1 = boxesList.get(0);
        TBox tBox2 = boxesList.get(1);

        assertThat(tBox1.getStaticResourcesList(), not(empty()));
        assertThat(tBox1.getRootfs().getLayerRefsList(), not(empty()));

        String BOX_1_LAYER_ID = LogrotatePatcherV1Base.makeId(tBox1.toBuilder(),
                LogrotatePatcherV1Base.LOG_ROTATE_APP_LAYER_ID);
        assertThat(tBox1.getRootfs().getLayerRefs(0).contains(BOX_1_LAYER_ID), equalTo(true));
        String BOX_1_RESOURCE_ID = LogrotatePatcherV1Base.makeId(tBox1.toBuilder(),
                LogrotatePatcherV1Base.LOG_ROTATE_CONFIG_RESOURCE_ID);

        assertThat(tBox1.getStaticResources(0).getResourceRef(), equalTo(BOX_1_RESOURCE_ID));

        assertThat(tBox2.getStaticResourcesList(), not(empty()));
        assertThat(tBox2.getRootfs().getLayerRefsList(), not(empty()));

        String BOX_2_LAYER_ID = LogrotatePatcherV1Base.makeId(tBox2.toBuilder(),
                LogrotatePatcherV1Base.LOG_ROTATE_APP_LAYER_ID);
        assertThat(tBox2.getRootfs().getLayerRefs(0).contains(BOX_2_LAYER_ID), equalTo(true));
        String BOX_2_RESOURCE_ID = LogrotatePatcherV1Base.makeId(tBox2.toBuilder(),
                LogrotatePatcherV1Base.LOG_ROTATE_CONFIG_RESOURCE_ID);
        assertThat(tBox2.getStaticResources(0).getResourceRef(),
                equalTo(BOX_2_RESOURCE_ID));

        var resources = podAgentSpec.getResources();

        assertThat(resources.getLayersList().size(), equalTo(2));
        assertThat(resources.getStaticResourcesList().size(), equalTo(2));

        assertThat(resources.getLayersList().stream().noneMatch(v -> v.getId().equals(BOX_1_LAYER_ID) || v.getId().equals(BOX_2_LAYER_ID)), equalTo(false));
        assertThat(resources.getStaticResourcesList().stream().noneMatch(v -> v.getId().equals(BOX_1_RESOURCE_ID) || v.getId().equals(BOX_2_RESOURCE_ID)), equalTo(false));
    }

    @Test
    public void patchWithLimits() {
        TPodTemplateSpec.Builder builder = TPodTemplateSpec.newBuilder();

        TBox box = TBox.newBuilder()
                .addVolumes(TMountedVolume.newBuilder().setVolumeRef(VOLUME_REF)).setVirtualDiskIdRef(ALLOCATION_ID_1).setId(BOX_ID).build();

        builder.getSpecBuilder().getPodAgentPayloadBuilder().getSpecBuilder().addBoxes(box);

        DeployUnitContext deployUnitContext = mock(DeployUnitContext.class);

        Map<String, LogrotateConfig> config = Map.of(BOX_ID, new LogrotateConfig("Config", 0));

        DeployUnitSpec deployUnitSpec = mock(DeployUnitSpec.class);
        when(deployUnitSpec.getLogrotateConfig()).thenReturn(config);
        when(deployUnitContext.getSpec()).thenReturn(deployUnitSpec);
        var podAgentSpec = patch(DEFAULT_PATCHER_CONTEXT, builder, deployUnitContext).getPodAgentSpec();
        TWorkload workload = podAgentSpec.getWorkloads(0);
        TUtilityContainer workloadStart = workload.getStart();

        assertThat(workloadStart.hasComputeResources(), equalTo(isLimited()));
        if (isLimited()) {
            assertThat(workloadStart.getComputeResources().getVcpuLimit(), not(equalTo(0)));
            assertThat(workloadStart.getComputeResources().getMemoryLimit(), not(equalTo(0)));
        }
    }



    @Test
    public void patchSpecWithTwoBoxTwoAllocationId() {
        TPodTemplateSpec.Builder builder = TPodTemplateSpec.newBuilder();

        TBox box1 = TBox.newBuilder()
                .addVolumes(TMountedVolume.newBuilder().setVolumeRef(VOLUME_REF))
                .setVirtualDiskIdRef(ALLOCATION_ID_1).setId(BOX_ID)
                .build();

        TBox box2 = TBox.newBuilder()
                .addVolumes(TMountedVolume.newBuilder().setVolumeRef(VOLUME_REF))
                .setVirtualDiskIdRef(ALLOCATION_ID_2).setId(BOX_ID_2)
                .build();

        builder.getSpecBuilder().getPodAgentPayloadBuilder().getSpecBuilder().addBoxes(box1).addBoxes(box2);

        DeployUnitContext deployUnitContext = mock(DeployUnitContext.class);

        Map<String, LogrotateConfig> config = Map.of(BOX_ID, new LogrotateConfig("Config", 0), BOX_ID_2,
                new LogrotateConfig("Config", 0));

        DeployUnitSpec deployUnitSpec = mock(DeployUnitSpec.class);
        when(deployUnitSpec.getLogrotateConfig()).thenReturn(config);
        when(deployUnitContext.getSpec()).thenReturn(deployUnitSpec);
        var podAgentSpec = patch(DEFAULT_PATCHER_CONTEXT, builder, deployUnitContext).getPodAgentSpec();

        List<TBox> boxesList = podAgentSpec.getBoxesList();
        assertThat(boxesList.size(), equalTo(2));

        TBox tBox1 = boxesList.get(0);
        TBox tBox2 = boxesList.get(1);

        assertThat(tBox1.getStaticResourcesList(), not(empty()));
        assertThat(tBox1.getRootfs().getLayerRefsList(), not(empty()));

        String BOX_1_LAYER_ID = LogrotatePatcherV1Base.makeId(tBox1.toBuilder(),
                LogrotatePatcherV1Base.LOG_ROTATE_APP_LAYER_ID);
        assertThat(tBox1.getRootfs().getLayerRefs(0).contains(BOX_1_LAYER_ID), equalTo(true));
        String BOX_1_RESOURCE_ID = LogrotatePatcherV1Base.makeId(tBox1.toBuilder(),
                LogrotatePatcherV1Base.LOG_ROTATE_CONFIG_RESOURCE_ID);

        assertThat(tBox1.getStaticResources(0).getResourceRef(), equalTo(BOX_1_RESOURCE_ID));

        assertThat(tBox2.getStaticResourcesList(), not(empty()));
        assertThat(tBox2.getRootfs().getLayerRefsList(), not(empty()));

        String BOX_2_LAYER_ID = LogrotatePatcherV1Base.makeId(tBox2.toBuilder(),
                LogrotatePatcherV1Base.LOG_ROTATE_APP_LAYER_ID);
        assertThat(tBox2.getRootfs().getLayerRefs(0).contains(BOX_2_LAYER_ID), equalTo(true));
        String BOX_2_RESOURCE_ID = LogrotatePatcherV1Base.makeId(tBox2.toBuilder(),
                LogrotatePatcherV1Base.LOG_ROTATE_CONFIG_RESOURCE_ID);
        assertThat(tBox2.getStaticResources(0).getResourceRef(),
                equalTo(BOX_2_RESOURCE_ID));

        var resources = podAgentSpec.getResources();

        assertThat(resources.getLayersList().size(), equalTo(2));
        assertThat(resources.getStaticResourcesList().size(), equalTo(2));

        assertThat(resources.getLayersList().stream().noneMatch(v -> v.getId().equals(BOX_1_LAYER_ID) || v.getId().equals(BOX_2_LAYER_ID)), equalTo(false));
        assertThat(resources.getStaticResourcesList().stream().noneMatch(v -> v.getId().equals(BOX_1_RESOURCE_ID) || v.getId().equals(BOX_2_RESOURCE_ID)), equalTo(false));
    }


    private static Stream<Arguments> addLogrotateLogsVolumeTestParameters() {
        Stream.Builder<Arguments> argStream = Stream.builder();

        POSSIBLE_VAR_LOG_MOUNT_POINTS.forEach(mp -> argStream.add(Arguments.of(Optional.of(mp), false)));
        argStream.add(Arguments.of(Optional.empty(), true));
        argStream.add(Arguments.of(Optional.of("any_mount_point"), true));

        return argStream.build();
    }

    @ParameterizedTest
    @MethodSource("addLogrotateLogsVolumeTestParameters")
    public void addLogrotateLogsVolumeTest(Optional<String> userVolumeMountPoint, boolean logrotateLogsVolumeExists) {
        TPodTemplateSpec.Builder builder = TPodTemplateSpec.newBuilder();

        TBox.Builder boxBuilder = TBox.newBuilder()
                .setRootfs(TRootfsVolume.newBuilder()
                        .addLayerRefs("some-layer"))
                .setId(BOX_ID);

        userVolumeMountPoint.ifPresent(mp -> boxBuilder.addVolumes(TMountedVolume.newBuilder().setVolumeRef(VOLUME_REF).setMountPoint(mp)));

        builder.getSpecBuilder().getPodAgentPayloadBuilder().getSpecBuilder().addBoxes(boxBuilder);

        DeployUnitContext deployUnitContext = mock(DeployUnitContext.class);

        Map<String, LogrotateConfig> config = Map.of(BOX_ID, new LogrotateConfig("Config", 0));
        DeployUnitSpec deployUnitSpec = mock(DeployUnitSpec.class);
        when(deployUnitSpec.getLogrotateConfig()).thenReturn(config);
        when(deployUnitContext.getSpec()).thenReturn(deployUnitSpec);
        var podAgentSpec = patch(DEFAULT_PATCHER_CONTEXT, builder, deployUnitContext).getPodAgentSpec();

        List<TBox> boxesList = podAgentSpec.getBoxesList();
        assertThat(boxesList.isEmpty(), equalTo(false));

        TBox box = boxesList.get(0);
        assertThat(box.getVolumesList().stream().anyMatch(v -> v.getMountPoint().equals(LOG_ROTATE_LOG_MOUNT_POINT)), equalTo(logrotateLogsVolumeExists));
    }

    protected boolean isLimited() {
        return true;
    }
}

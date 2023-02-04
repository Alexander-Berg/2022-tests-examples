package ru.yandex.infra.stage.podspecs.patcher.tvm;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import ru.yandex.infra.stage.StageValidatorImpl;
import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.deployunit.DeployUnitContext;
import ru.yandex.infra.stage.dto.AllComputeResources;
import ru.yandex.infra.stage.dto.Checksum;
import ru.yandex.infra.stage.dto.DeployUnitSpec;
import ru.yandex.infra.stage.dto.DownloadableResource;
import ru.yandex.infra.stage.dto.SecretSelector;
import ru.yandex.infra.stage.dto.SidecarVolumeSettings;
import ru.yandex.infra.stage.dto.TvmApp;
import ru.yandex.infra.stage.dto.TvmClient;
import ru.yandex.infra.stage.dto.TvmConfig;
import ru.yandex.infra.stage.podspecs.FixedResourceSupplier;
import ru.yandex.infra.stage.podspecs.PatcherTestUtils;
import ru.yandex.infra.stage.podspecs.PodSpecUtils;
import ru.yandex.infra.stage.podspecs.ResourceSupplier;
import ru.yandex.infra.stage.podspecs.ResourceWithMeta;
import ru.yandex.infra.stage.podspecs.SandboxReleaseGetter;
import ru.yandex.infra.stage.podspecs.patcher.PatcherTestBase;
import ru.yandex.infra.stage.util.AssertUtils;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.inside.yt.kosher.ytree.YTreeNode;
import ru.yandex.yp.client.api.DataModel;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.pods.TBox;
import ru.yandex.yp.client.pods.TEnvVar;
import ru.yandex.yp.client.pods.TFile;
import ru.yandex.yp.client.pods.TLayer;
import ru.yandex.yp.client.pods.TPodAgentSpec;
import ru.yandex.yp.client.pods.TPodAgentSpecOrBuilder;
import ru.yandex.yp.client.pods.TResource;
import ru.yandex.yp.client.pods.TVolume;
import ru.yandex.yp.client.pods.TWorkload;
import ru.yandex.yt.ytree.TAttributeDictionary;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static ru.yandex.infra.stage.TestData.BLACKBOX_ENVIRONMENT;
import static ru.yandex.infra.stage.TestData.DEFAULT_SIDECAR_RESOURCE_INFO;
import static ru.yandex.infra.stage.TestData.DEFAULT_SIDECAR_REVISION;
import static ru.yandex.infra.stage.TestData.DEFAULT_STAGE_CONTEXT;
import static ru.yandex.infra.stage.TestData.DEFAULT_UNIT_CONTEXT;
import static ru.yandex.infra.stage.TestData.TVM_CLIENT;
import static ru.yandex.infra.stage.TestData.TVM_CONFIG;
import static ru.yandex.infra.stage.TestData.TVM_CONFIG_WITH_NO_DESTINATIONS;
import static ru.yandex.infra.stage.TestData.USED_BY_INFRA_VOLUME_REQUEST;
import static ru.yandex.infra.stage.podspecs.PatcherTestUtils.groupById;
import static ru.yandex.infra.stage.podspecs.PatcherTestUtils.testIds;
import static ru.yandex.infra.stage.podspecs.PodSpecUtils.DISABLE_DISK_ISOLATION_LABEL_ATTRIBUTE;
import static ru.yandex.infra.stage.podspecs.PodSpecUtils.MEGABYTE;
import static ru.yandex.infra.stage.podspecs.PodSpecUtils.SYSTEM_BOX_SPECIFIC_TYPE;
import static ru.yandex.infra.stage.podspecs.patcher.tvm.TvmPatcherUtils.TVM_BOX_ID;
import static ru.yandex.infra.stage.podspecs.patcher.tvm.TvmPatcherUtils.TVM_CONF_RESOURCE_ID;
import static ru.yandex.infra.stage.podspecs.patcher.tvm.TvmPatcherUtils.TVM_THREAD_LIMIT;
import static ru.yandex.infra.stage.podspecs.patcher.tvm.TvmPatcherV1Base.DEPLOY_TVM_CONFIG_ENV_NAME;
import static ru.yandex.infra.stage.podspecs.patcher.tvm.TvmPatcherV1Base.DISK_SIZE_MB_DEFAULT;
import static ru.yandex.infra.stage.podspecs.patcher.tvm.TvmPatcherV1Base.TVM_CONF_FILE_NAME;
import static ru.yandex.infra.stage.podspecs.patcher.tvm.TvmPatcherV1Base.TVM_ENABLED_KEY;
import static ru.yandex.infra.stage.podspecs.patcher.tvm.TvmPatcherV1Base.TVM_ENABLED_VALUE;
import static ru.yandex.infra.stage.podspecs.patcher.tvm.TvmPatcherV1Base.TVM_INSTALLATION_LABEL;
import static ru.yandex.infra.stage.podspecs.patcher.tvm.TvmPatcherV1Base.TVM_KEY;
import static ru.yandex.infra.stage.podspecs.patcher.tvm.TvmPatcherV1Base.TVM_STAGE_ID_LABEL;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

abstract class TvmPatcherV1BaseTest extends PatcherTestBase<TvmPatcherV1Context> {
    protected static final int CUSTOM_CPU_LIMIT = 200;
    protected static final int CUSTOM_MEMORY_LIMIT_MB = 64;
    private static final ResourceSupplier BASE_LAYER_SUPPLIER =
            FixedResourceSupplier.withMeta(TestData.DOWNLOADABLE_RESOURCE,
                    TestData.RESOURCE_META);
    private static final ResourceSupplier TVM_LAYER_SUPPLIER =
            FixedResourceSupplier.withMeta(TestData.DOWNLOADABLE_RESOURCE2,
                    TestData.RESOURCE_META);
    private static final String DEFAULT_INSTALLATION = "testing";
    private static final DeployUnitContext TVM_UNIT_CONTEXT =
            DEFAULT_UNIT_CONTEXT.withTvmConfig(TVM_CONFIG);
    private static final DeployUnitContext TVM_UNIT_CONTEXT_WITH_NO_DESTINATIONS =
            DEFAULT_UNIT_CONTEXT.withTvmConfig(TVM_CONFIG_WITH_NO_DESTINATIONS);
    private static final AllComputeResources TVM_COMPUTE_REQUESTS =
            new AllComputeResources(TVM_CONFIG.getCpuLimit(), TVM_CONFIG.getMemoryLimitMb() * MEGABYTE,
                    DISK_SIZE_MB_DEFAULT * MEGABYTE, 150);
    private static final int RELEASE_GETTER_TIMEOUT_SECONDS = 5;
    private static final TvmPatcherV1Context DEFAULT_PATCHER_CONTEXT = new TvmPatcherV1Context(
            ImmutableMap.of("test", 1),
            BASE_LAYER_SUPPLIER,
            TVM_LAYER_SUPPLIER,
            DISK_SIZE_MB_DEFAULT,
            Optional.empty(),
            Optional.empty(),
            DEFAULT_INSTALLATION,
            false,
            RELEASE_GETTER_TIMEOUT_SECONDS);

    private static YTreeNode createTvmLabelsAttribute() {
        YTreeNode deliveryNode = new YTreeBuilder().beginMap()
                .key(TVM_STAGE_ID_LABEL).value(DEFAULT_STAGE_CONTEXT.getStageId())
                .key(TVM_INSTALLATION_LABEL).value(DEFAULT_INSTALLATION)
                .key(TVM_ENABLED_KEY).value(TVM_ENABLED_VALUE)
                .endMap().build();

        return new YTreeBuilder().beginMap()
                .key(TVM_KEY).value(deliveryNode)
                .endMap().build();
    }

    private TBox getTvmBox(TPodAgentSpecOrBuilder podAgentSpec) {
        return podAgentSpec
                .getBoxesList()
                .stream()
                .filter(box -> box.getId().equals(getTvmBoxId()))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void addTvmLabels() {
        var labels = patch(DEFAULT_PATCHER_CONTEXT, createPodSpecBuilder(), TVM_UNIT_CONTEXT).getLabels();
        assertThat(labels, equalTo(createTvmLabelsAttribute()));
    }

    @Test
    void getInfraComponentFromSpec() {
        SandboxReleaseGetter sandboxReleaseGetter = Mockito.mock(SandboxReleaseGetter.class);
        Checksum expectedChecksum = new Checksum("tvmPatcherTest", Checksum.Type.MD5);
        DownloadableResource downloadableResource = new DownloadableResource("getInfraComponentFromSpec",
                expectedChecksum);
        CompletableFuture<ResourceWithMeta> resourceWithMetaCompletableFuture = new CompletableFuture<>();
        resourceWithMetaCompletableFuture.complete(new ResourceWithMeta(downloadableResource, Optional.empty()));
        Mockito.doReturn(resourceWithMetaCompletableFuture)
                .when(sandboxReleaseGetter).getReleaseByResourceId(DEFAULT_SIDECAR_REVISION, true);

        DeployUnitContext deployUnitContext = TVM_UNIT_CONTEXT.withDeployUnitSpec(
                DeployUnitSpec::withTvmToolResourceInfo, DEFAULT_SIDECAR_RESOURCE_INFO
        );
        ResourceSupplier fixedResourceSupplier =
                FixedResourceSupplier.withSupplierAndMeta(TestData.DOWNLOADABLE_RESOURCE2,
                        TestData.RESOURCE_META, sandboxReleaseGetter);

        var podAgentSpec = patch(
                DEFAULT_PATCHER_CONTEXT.withTvmLayerSupplier(fixedResourceSupplier),
                createPodSpecBuilder(),
                deployUnitContext
        ).getPodAgentSpec();

        String checksum = podAgentSpec.getResources().getLayersList().get(1).getChecksum();
        assertThat(checksum, equalTo(expectedChecksum.toAgentFormat()));
    }

    @Test
    void notAddLabelsIfTmvIsAbsent() {
        var labels = patch(DEFAULT_PATCHER_CONTEXT, createPodSpecBuilder(), DEFAULT_UNIT_CONTEXT).getLabels();
        assertThat("labels should be absent", labels.getAttributes().isEmpty());
    }

    @Test
    void notAddLabelsIfTmvIsDisabled() {
        var tvmConfig = new TvmConfigBuilder()
                .setMode(TvmConfig.Mode.DISABLED)
                .setClientPort(OptionalInt.of(4000))
                .build();

        var labels = patch(
                DEFAULT_PATCHER_CONTEXT,
                createPodSpecBuilder(),
                DEFAULT_UNIT_CONTEXT.withTvmConfig(tvmConfig)
        ).getLabels();

        assertThat("labels should be absent", labels.getAttributes().isEmpty());
    }

    @Test
    void addTvmTool() {
        String diskVolumeAllocationId = "allocation_id";
        Set<String> diskVolumeRequestInSpec = ImmutableSet.of(diskVolumeAllocationId);
        String virtualDiskRefInSpec = diskVolumeAllocationId;

        addTvmToolScenario(TPodTemplateSpec.newBuilder(),
                diskVolumeAllocationId,
                diskVolumeRequestInSpec,
                virtualDiskRefInSpec,
                false);
    }

    @Test
    void addTvmToolWithNoDestinations() {
        String diskVolumeAllocationId = "allocation_id";
        Set<String> diskVolumeRequestInSpec = ImmutableSet.of(diskVolumeAllocationId);
        String virtualDiskRefInSpec = diskVolumeAllocationId;

        addTvmToolScenario(TPodTemplateSpec.newBuilder(),
                diskVolumeAllocationId,
                diskVolumeRequestInSpec,
                virtualDiskRefInSpec,
                true);
    }

    @Test
    void addTvmToolWhenDisableDiskIsolationLabelInSpec() {
        TPodTemplateSpec.Builder templateSpec = createPodSpecBuilder().setLabels(TAttributeDictionary.newBuilder()
                .addAttributes(DISABLE_DISK_ISOLATION_LABEL_ATTRIBUTE));

        String diskVolumeAllocationId = "allocation_id";
        Set<String> diskVolumeRequestInSpec = ImmutableSet.of();
        String virtualDiskRefInSpec = "";

        addTvmToolScenario(templateSpec, diskVolumeAllocationId, diskVolumeRequestInSpec, virtualDiskRefInSpec, false);
    }

    @Test
    void customTvmToolTest() {
        ResourceSupplier unknownResource = FixedResourceSupplier.withoutMeta(new DownloadableResource("unknown", Checksum.EMPTY));

        var patcherContext = DEFAULT_PATCHER_CONTEXT.toBuilder()
                .withTvmLayerSupplier(unknownResource)
                .withBaseLayerSupplier(unknownResource)
                .build();

        var tvmConfig = new TvmConfigBuilder()
                .setTvmtoolLayer(Optional.of(TestData.DOWNLOADABLE_RESOURCE2))
                .build();

        var podAgentSpec = patch(
                patcherContext,
                createPodSpecBuilder(),
                DEFAULT_UNIT_CONTEXT.withTvmConfig(tvmConfig)
        ).getPodAgentSpec();

        var actualResourceUrl = podAgentSpec.getResources().getLayersList().stream()
                .filter(layer -> layer.getId().equals(TvmPatcherV1Base.TVM_LAYER_ID)).findAny().orElseThrow().getUrl();
        assertThat(actualResourceUrl, equalTo(TestData.DOWNLOADABLE_RESOURCE2.getUrl()));
    }

    @Test
    void solomonConfigGenerationTest() {
        String resultTvmConfig = generateTVMSectionByConfig(
                new TvmConfigBuilder()
                .setTvmtoolLayer(Optional.of(TestData.DOWNLOADABLE_RESOURCE2))
                .setStageTvmId(OptionalInt.of(1))
                .setSolomonTvmId(OptionalInt.of(2))
                .setMonitoringPort(OptionalInt.of(3))
        );

        String solomonSection =  "\"solomon\" : {\n" +
                "    \"stage_tvm_id\" : 1,\n" +
                "    \"solomon_tvm_id\" : 2,\n" +
                "    \"port\" : 3\n" +
                "  }";
        assertThat(resultTvmConfig.contains(solomonSection), equalTo(true));
    }

    @Test
    void rolesForIdmSlugTest() {
        String section = "\"clients\" : {\n" +
                "    \"alias1\" : {\n" +
                "      \"self_tvm_id\" : 1,\n" +
                "      \"secret\" : \"env:TVM_CLIENT_SECRET_1\",\n" +
                "      \"roles_for_idm_slug\" : \"blah\",\n" +
                "      \"dsts\" : {\n" +
                "        \"alias2\" : {\n" +
                "          \"dst_id\" : 2\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  },";

        checkConfigGenerationForSection("blah", false, true, section);
        checkConfigGenerationForSection("", false, false, section);
    }

    @Test
    void useSystemCerts() {
        String section = "\"BbEnvType\" : 1,\n" +
                "  \"use_system_certs\" : true,\n" +
                "  \"port\" : 2,";

        checkConfigGenerationForSection("", false, false, section);
        checkConfigGenerationForSection("", true, true, section);
    }

    private void checkConfigGenerationForSection(String slug, boolean useSystemCetrs, boolean shouldContain, String clientSection) {
        TvmClient tvmClient = new TvmClient(new SecretSelector("alias", "key"),
                new TvmApp(1, "alias1"), ImmutableList.of(new TvmApp(2, "alias2")),
                slug);

        String resultTvmConfig = generateTVMSectionByConfig(new TvmConfigBuilder()
                .setTvmtoolLayer(Optional.of(TestData.DOWNLOADABLE_RESOURCE2))
                .setStageTvmId(OptionalInt.of(1))
                .setSolomonTvmId(OptionalInt.of(2))
                .setMonitoringPort(OptionalInt.of(3))
                .setClients(ImmutableList.of(tvmClient))
                .setUseSystemCerts(useSystemCetrs));

        assertThat(resultTvmConfig.contains(clientSection), equalTo(shouldContain));
    }

    @NotNull
    private String generateTVMSectionByConfig(TvmConfigBuilder tvmConfigBuilder) {
        ResourceSupplier unknownResource = FixedResourceSupplier.withoutMeta(new DownloadableResource("unknown", Checksum.EMPTY));
        var patcherContext = DEFAULT_PATCHER_CONTEXT
                .withTvmLayerSupplier(unknownResource);

        var tvmConfig = tvmConfigBuilder.build();

        var podAgentSpec = patch(
                patcherContext,
                createPodSpecBuilder(),
                DEFAULT_UNIT_CONTEXT.withTvmConfig(tvmConfig)
        ).getPodAgentSpec();

        return podAgentSpec.getResources().getStaticResourcesList()
                .get(0).getFiles().getFiles(0).getRawData();
    }

    @Test
    void patchTvmBoxWithThreadLimit() {
        var podAgentSpec = patch(DEFAULT_PATCHER_CONTEXT, createPodSpecBuilder(), TVM_UNIT_CONTEXT).getPodAgentSpec();
        var tvmBox = getTvmBox(podAgentSpec);
        assertThat(tvmBox.getComputeResources().getThreadLimit(), equalTo(TVM_THREAD_LIMIT));
    }

    @Test
    void patchTvmBoxWithCpuAndMemoryLimits() {
        var tvmConfig = new TvmConfigBuilder()
                .setCpuLimit(OptionalInt.of(CUSTOM_CPU_LIMIT))
                .setClientPort(OptionalInt.of(4000))
                .setMemoryLimitMb(OptionalInt.of(CUSTOM_MEMORY_LIMIT_MB))
                .build();

        var patchResult = patch(
                DEFAULT_PATCHER_CONTEXT,
                createPodSpecBuilder(),
                DEFAULT_UNIT_CONTEXT.withTvmConfig(tvmConfig)
        );

        var podSpec = patchResult.getPodSpec();
        var actualSpecResources = podSpec.getResourceRequests();

        AllComputeResources expectedSpecResources =
                new AllComputeResources(CUSTOM_CPU_LIMIT, CUSTOM_MEMORY_LIMIT_MB * MEGABYTE, 0, 0);
        AssertUtils.assertResourceRequestEquals(actualSpecResources, expectedSpecResources);

        var tvmBox = getTvmBox(patchResult.getPodAgentSpec());
        ensureTvmBoxLimits(tvmBox);
    }

    protected void addTvmConfigEnvVarScenario(boolean expectedTvmConfigEnvVarAdded) {
        var tvmConfig = new TvmConfigBuilder()
                .setCpuLimit(OptionalInt.of(CUSTOM_CPU_LIMIT))
                .setClientPort(OptionalInt.of(4000))
                .setMemoryLimitMb(OptionalInt.of(CUSTOM_MEMORY_LIMIT_MB))
                .build();

        String boxId = "box-id";
        String workloadId = "workload-id";

        var podSpecBuilder = createPodSpecBuilder();

        podSpecBuilder.getSpecBuilder()
                .getPodAgentPayloadBuilder()
                .getSpecBuilder()
                .addBoxes(TBox.newBuilder().setId(boxId).build())
                .addWorkloads(TWorkload.newBuilder().setId(workloadId).setBoxRef(boxId).build());

        var patchResult = patch(
                DEFAULT_PATCHER_CONTEXT,
                podSpecBuilder,
                DEFAULT_UNIT_CONTEXT.withTvmConfig(tvmConfig)
        );

        TResource configRes = patchResult.getPodAgentSpec()
                .getResources().getStaticResourcesList().stream()
                .filter(res -> TVM_CONF_RESOURCE_ID.equals(res.getId()))
                .findFirst().orElseThrow();

        TFile configFile = configRes.getFiles().getFilesList().stream()
                .filter(file -> TVM_CONF_FILE_NAME.equals(file.getFileName()))
                .findFirst().orElseThrow();

        String configStr = configFile.getRawData();

        TBox box = PatcherTestUtils.getBoxById(patchResult.getPodAgentSpec(), boxId).orElseThrow();
        TWorkload workload = patchResult.getPodAgentSpec().getWorkloadsList().stream()
                .filter(tWorkload -> workloadId.equals(tWorkload.getId()))
                .filter(tWorkload -> boxId.equals(tWorkload.getBoxRef())).findFirst().orElseThrow();

        assertThat(PatcherTestUtils.hasLiteralEnv(box, DEPLOY_TVM_CONFIG_ENV_NAME, configStr), equalTo(false));
        assertThat(PatcherTestUtils.hasLiteralEnv(workload, DEPLOY_TVM_CONFIG_ENV_NAME, configStr),
                equalTo(expectedTvmConfigEnvVarAdded));
    }

    @Test
    void setTvmEnvInUserBox() {
        String userBoxId = "box";
        TPodTemplateSpec.Builder templateSpecBuilder = createPodSpecBuilder();
        templateSpecBuilder
                .getSpecBuilder()
                .getPodAgentPayloadBuilder()
                .getSpecBuilder()
                .addBoxes(TBox.newBuilder()
                        .setId(userBoxId)
                );

        var podSpec = patch(DEFAULT_PATCHER_CONTEXT, templateSpecBuilder, TVM_UNIT_CONTEXT).getPodSpec();

        TBox userBox = podSpec.getPodAgentPayload()
                .getSpec()
                .getBoxesList()
                .stream()
                .filter(box -> box.getId().equals(userBoxId))
                .findAny()
                .orElseThrow();

        assertThat(PatcherTestUtils.hasLiteralEnv(userBox, "DEPLOY_TVM_TOOL_URL", String.format("http" +
                        "://localhost:%d"
                , TVM_CONFIG.getClientPort())), equalTo(true));
    }

    private void addTvmToolScenario(TPodTemplateSpec.Builder templateSpec,
                                    String diskVolumeAllocationId,
                                    Set<String> diskVolumeRequestInSpec,
                                    String virtualDiskRefInSpec,
                                    boolean useContextWithoutDestinations) {
        templateSpec.getSpecBuilder()
                .addDiskVolumeRequests(USED_BY_INFRA_VOLUME_REQUEST);

        var patcherContext = DEFAULT_PATCHER_CONTEXT.toBuilder()
                .withDiskVolumeAllocationId(diskVolumeAllocationId)
                .withAllSidecarDiskAllocationIds(ImmutableList.of(diskVolumeAllocationId))
                .withPatchBoxSpecificType(true)
                .build();

        var tvmUnitContext = useContextWithoutDestinations ? TVM_UNIT_CONTEXT_WITH_NO_DESTINATIONS : TVM_UNIT_CONTEXT;
        var podSpec = patch(patcherContext, templateSpec, tvmUnitContext).getPodSpec();
        testIds(podSpec.getDiskVolumeRequestsList(), diskVolumeRequestInSpec,
                DataModel.TPodSpec.TDiskVolumeRequest::getId);

        List<DataModel.TPodSpec.TDiskVolumeRequest> volumeRequests = podSpec.getDiskVolumeRequestsList().stream()
                .filter(r -> r.getId().equals(USED_BY_INFRA_VOLUME_REQUEST.getId()))
                .collect(Collectors.toList());
        assertThat(volumeRequests, hasSize(1));

        AssertUtils.assertResourceRequestEquals(podSpec.getResourceRequests(), TVM_COMPUTE_REQUESTS);

        TPodAgentSpec agentSpec = podSpec.getPodAgentPayload().getSpec();

        Map<String, TLayer> layerMap = groupById(agentSpec.getResources().getLayersList(), TLayer::getId);
        assertThat(layerMap.keySet(), containsInAnyOrder(TvmPatcherUtils.TVM_BASE_LAYER_ID,
                TvmPatcherV1Base.TVM_LAYER_ID));
        assertThat(layerMap.get(TvmPatcherUtils.TVM_BASE_LAYER_ID).getMeta(),
                equalTo(PodSpecUtils.resourceMeta(TestData.RESOURCE_META)));

        assertThat(layerMap.get(TvmPatcherUtils.TVM_BASE_LAYER_ID).getVirtualDiskIdRef(),
                equalTo(virtualDiskRefInSpec));
        assertThat(layerMap.get(TvmPatcherV1Base.TVM_LAYER_ID).getVirtualDiskIdRef(), equalTo(virtualDiskRefInSpec));

        Set<String> expectedVolumeIds = ImmutableSet.of(TvmPatcherUtils.TVM_LOG_VOLUME_ID,
                TvmPatcherUtils.TVM_CACHE_VOLUME_ID);
        testIds(agentSpec.getVolumesList(), expectedVolumeIds, TVolume::getId);

        Map<String, TVolume> volumeMap = groupById(agentSpec.getVolumesList(), TVolume::getId);
        assertThat(volumeMap.get(TvmPatcherUtils.TVM_CACHE_VOLUME_ID).getVirtualDiskIdRef(),
                equalTo(virtualDiskRefInSpec));
        assertThat(volumeMap.get(TvmPatcherUtils.TVM_LOG_VOLUME_ID).getVirtualDiskIdRef(),
                equalTo(virtualDiskRefInSpec));

        Map<String, TResource> resourceMap = groupById(agentSpec.getResources().getStaticResourcesList(),
                TResource::getId);
        assertThat(resourceMap.keySet(), containsInAnyOrder(TvmPatcherUtils.TVM_CONF_RESOURCE_ID));

        assertThat(resourceMap.get(TvmPatcherUtils.TVM_CONF_RESOURCE_ID).getVirtualDiskIdRef(),
                equalTo(virtualDiskRefInSpec));

        Map<String, TBox> boxMap = groupById(agentSpec.getBoxesList(), TBox::getId);
        assertThat(boxMap.keySet(), containsInAnyOrder(getTvmBoxId()));
        // Order of layers is important
        assertThat(boxMap.get(getTvmBoxId()).getRootfs().getLayerRefsList(),
                contains(TvmPatcherV1Base.TVM_LAYER_ID,
                        TvmPatcherUtils.TVM_BASE_LAYER_ID));

        assertThat(boxMap.get(getTvmBoxId()).getVirtualDiskIdRef(), equalTo(virtualDiskRefInSpec));
        assertThat(boxMap.get(getTvmBoxId()).getSpecificType(), equalTo(SYSTEM_BOX_SPECIFIC_TYPE));

        testIds(agentSpec.getWorkloadsList(), StageValidatorImpl.TVM_ALL_WORKLOAD_IDS, TWorkload::getId);

        TWorkload workload = agentSpec.getWorkloadsList().get(0);
        String expectedSecretEnv = TVM_CLIENT.getTvmClientSecretEnvName();
        List<TEnvVar> secretEnvs = workload.getEnvList().stream()
                .filter(e -> e.getValue().hasSecretEnv() && e.getName().equals(expectedSecretEnv))
                .collect(Collectors.toList());
        assertThat(secretEnvs, hasSize(useContextWithoutDestinations ? 0 : 1));
    }

    protected void ensureTvmBoxLimits(TBox tvmBox) {
        var actualBoxResources = tvmBox.getComputeResources();
        assertThatEquals(actualBoxResources.getMemoryLimit(), CUSTOM_MEMORY_LIMIT_MB * MEGABYTE);
        assertThatEquals(actualBoxResources.getAnonymousMemoryLimit(), CUSTOM_MEMORY_LIMIT_MB * MEGABYTE * 9 / 10);
        assertThatEquals(actualBoxResources.getVcpuLimit(), (long) CUSTOM_CPU_LIMIT);
    }

    protected String getTvmBoxId() {
        return TVM_BOX_ID;
    }

    private static class TvmConfigBuilder {
        private TvmConfig.Mode mode;
        private String blackboxEnvironment;
        private List<TvmClient> clients;
        private OptionalInt clientPort;
        private OptionalInt cpuLimit;
        private OptionalInt memoryLimitMb;
        private Optional<DownloadableResource> tvmtoolLayer;
        private OptionalInt stageTvmId;
        private OptionalInt solomonTvmId;
        private OptionalInt monitoringPort;
        private Optional<SidecarVolumeSettings> sidecarVolumeSettings;
        private boolean useSystemCerts;

        public TvmConfigBuilder setMode(TvmConfig.Mode mode) {
            this.mode = mode;
            return this;
        }

        public TvmConfigBuilder setBlackboxEnvironment(String blackboxEnvironment) {
            this.blackboxEnvironment = blackboxEnvironment;
            return this;
        }

        public TvmConfigBuilder setClients(List<TvmClient> clients) {
            this.clients = clients;
            return this;
        }

        public TvmConfigBuilder setClientPort(OptionalInt clientPort) {
            this.clientPort = clientPort;
            return this;
        }

        public TvmConfigBuilder setCpuLimit(OptionalInt cpuLimit) {
            this.cpuLimit = cpuLimit;
            return this;
        }

        public TvmConfigBuilder setMemoryLimitMb(OptionalInt memoryLimitMb) {
            this.memoryLimitMb = memoryLimitMb;
            return this;
        }

        public TvmConfigBuilder setTvmtoolLayer(Optional<DownloadableResource> tvmtoolLayer) {
            this.tvmtoolLayer = tvmtoolLayer;
            return this;
        }

        public TvmConfigBuilder setStageTvmId(OptionalInt stageTvmId) {
            this.stageTvmId = stageTvmId;
            return this;
        }

        public TvmConfigBuilder setSolomonTvmId(OptionalInt solomonTvmId) {
            this.solomonTvmId = solomonTvmId;
            return this;
        }

        public TvmConfigBuilder setMonitoringPort(OptionalInt monitoringPort) {
            this.monitoringPort = monitoringPort;
            return this;
        }

        public TvmConfigBuilder setSidecarVolumeSettings(Optional<SidecarVolumeSettings> sidecarVolumeSettings) {
            this.sidecarVolumeSettings = sidecarVolumeSettings;
            return this;
        }

        public TvmConfigBuilder setUseSystemCerts(boolean useSystemCerts){
            this.useSystemCerts = useSystemCerts;
            return this;
        }

        TvmConfigBuilder() {
            mode = TvmConfig.Mode.ENABLED;
            blackboxEnvironment = BLACKBOX_ENVIRONMENT;
            clients = ImmutableList.of(TVM_CLIENT);
            clientPort = OptionalInt.empty();
            cpuLimit = OptionalInt.empty();
            memoryLimitMb = OptionalInt.empty();
            tvmtoolLayer = Optional.empty();
            stageTvmId = OptionalInt.empty();
            solomonTvmId = OptionalInt.empty();
            monitoringPort = OptionalInt.empty();
            sidecarVolumeSettings = Optional.empty();
            useSystemCerts = false;
        }

        TvmConfig build() {
            return new TvmConfig(mode, blackboxEnvironment, clients, clientPort,
                    cpuLimit, memoryLimitMb, tvmtoolLayer, stageTvmId,
                    solomonTvmId, monitoringPort, sidecarVolumeSettings, useSystemCerts);
        }
    }
}

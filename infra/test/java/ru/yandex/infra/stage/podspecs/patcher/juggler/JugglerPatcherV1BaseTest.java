package ru.yandex.infra.stage.podspecs.patcher.juggler;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.dto.AllComputeResources;
import ru.yandex.infra.stage.dto.BoxJugglerConfig;
import ru.yandex.infra.stage.dto.DeployUnitSpec;
import ru.yandex.infra.stage.dto.DownloadableResource;
import ru.yandex.infra.stage.podspecs.FixedResourceSupplier;
import ru.yandex.infra.stage.podspecs.PatcherTestUtils;
import ru.yandex.infra.stage.podspecs.PodSpecUtils;
import ru.yandex.infra.stage.podspecs.ResourceSupplier;
import ru.yandex.infra.stage.podspecs.patcher.PatcherTestBase;
import ru.yandex.infra.stage.util.AssertUtils;
import ru.yandex.yp.client.api.DataModel;
import ru.yandex.yp.client.api.Enums;
import ru.yandex.yp.client.api.TMonitoringSubagentEndpoint;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.pods.TBox;
import ru.yandex.yp.client.pods.TComputeResources;
import ru.yandex.yp.client.pods.TMountedStaticResource;
import ru.yandex.yp.client.pods.TPodAgentSpec;
import ru.yandex.yp.client.pods.TResource;
import ru.yandex.yp.client.pods.TWorkload;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static ru.yandex.infra.stage.podspecs.patcher.juggler.JugglerPatcherV1Base.JUGGLER_DEFAULT_BINARY_ID;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

abstract class JugglerPatcherV1BaseTest extends PatcherTestBase<JugglerPatcherV1Context> {

    private static final DownloadableResource DEFAULT_RESOURCE = TestData.DOWNLOADABLE_RESOURCE;

    private static final DataModel.TPodSpec.TDiskVolumeRequest DEFAULT_SIDECAR_VOLUME_REQUEST =
            TestData.USED_BY_INFRA_VOLUME_REQUEST;

    protected static class BoxTestInfo {
        private final int index;
        private final String id;
        private final DataModel.TPodSpec.TDiskVolumeRequest volumeRequest;
        private final DownloadableResource resource;
        private final DownloadableResource expectedResource;

        BoxTestInfo(int index,
                    DataModel.TPodSpec.TDiskVolumeRequest volumeRequest,
                    DownloadableResource resource,
                    DownloadableResource expectedResource) {
            this.index = index;
            this.id = "box-id-" + index;
            this.volumeRequest = volumeRequest;
            this.resource = resource;
            this.expectedResource = expectedResource;
        }

        public int getIndex() {
            return index;
        }

        public String getId() {
            return id;
        }

        public DataModel.TPodSpec.TDiskVolumeRequest getVolumeRequest() {
            return volumeRequest;
        }

        public DownloadableResource getResource() {
            return resource;
        }
        public DownloadableResource getExpectedResource() {
            return expectedResource;
        }

        public String getAllocationId() {
            return volumeRequest.getId();
        }
    }

    private static final BoxTestInfo DEFAULT_BOX = new BoxTestInfo(1, TestData.NOT_USED_BY_INFRA_VOLUME_REQUEST,
            TestData.DOWNLOADABLE_RESOURCE, TestData.DOWNLOADABLE_RESOURCE);
    private static final BoxTestInfo DEFAULT_BOX_2 = new BoxTestInfo(2, TestData.NOT_USED_BY_INFRA_VOLUME_REQUEST_2,
            TestData.DOWNLOADABLE_SBR_RESOURCE, TestData.DOWNLOADABLE_RESOURCE2);

    private static final String EMPTY_ALLOCATION_ID = "";
    private static final String DEFAULT_DISK_ALLOCATION_ID = DEFAULT_BOX.getAllocationId();

    protected static final List<BoxTestInfo> DEFAULT_BOXES = ImmutableList.of(DEFAULT_BOX, DEFAULT_BOX_2);

    private static final ResourceSupplier DEFAULT_SUPPLIER = FixedResourceSupplier.withMeta(DEFAULT_RESOURCE,
            TestData.RESOURCE_META);

    private static final JugglerPatcherV1Context DEFAULT_PATCHER_CONTEXT = new JugglerPatcherV1Context(
            DEFAULT_SUPPLIER, Optional.empty());

    enum BoxAllocationMode {
        WITHOUT_ALLOCATIONS {
            @Override
            void patchBoxBuilder(TBox.Builder boxBuilder, BoxTestInfo boxTestInfo) {

            }

            @Override
            public List<DataModel.TPodSpec.TDiskVolumeRequest> getDiskVolumeRequests(List<BoxTestInfo> boxes) {
                return ImmutableList.of();
            }

            @Override
            public int getBoxResultResourceIndex(BoxTestInfo boxTestInfo) {
                return boxTestInfo.getIndex();
            }

            @Override
            public String getBoxResultAllocationId(BoxTestInfo boxTestInfo) {
                return EMPTY_ALLOCATION_ID;
            }
        },

        WITH_ALLOCATIONS {
            @Override
            void patchBoxBuilder(TBox.Builder boxBuilder, BoxTestInfo boxTestInfo) {
                boxBuilder.setVirtualDiskIdRef(boxTestInfo.getAllocationId());
            }

            @Override
            public List<DataModel.TPodSpec.TDiskVolumeRequest> getDiskVolumeRequests(List<BoxTestInfo> boxes) {
                return boxes.stream().map(BoxTestInfo::getVolumeRequest).collect(Collectors.toUnmodifiableList());
            }

            @Override
            public int getBoxResultResourceIndex(BoxTestInfo boxTestInfo) {
                return 1;
            }

            @Override
            public String getBoxResultAllocationId(BoxTestInfo boxTestInfo) {
                return boxTestInfo.getAllocationId();
            }
        };

        abstract void patchBoxBuilder(TBox.Builder boxBuilder, BoxTestInfo boxTestInfo);

        public TBox.Builder createBox(BoxTestInfo boxTestInfo) {
            var boxBuilder = createBoxBuilder(boxTestInfo.id);
            patchBoxBuilder(boxBuilder, boxTestInfo);
            return boxBuilder;
        }

        public abstract List<DataModel.TPodSpec.TDiskVolumeRequest> getDiskVolumeRequests(List<BoxTestInfo> boxes);

        public abstract String getBoxResultAllocationId(BoxTestInfo boxTestInfo);

        public abstract int getBoxResultResourceIndex(BoxTestInfo boxTestInfo);
    }

    protected abstract AllComputeResources getExpectedAdditionalBoxResources();
    protected abstract AllComputeResources getExpectedBoxGuaranteeResources(TBox boxSpec);


    private static TPodAgentSpec.Builder createPodAgentBuilder() {
        return TPodAgentSpec.newBuilder();
    }

    private static TBox.Builder createBoxBuilder(String boxId) {
        return TBox.newBuilder().setId(boxId);
    }

    private static TPodTemplateSpec.Builder createPodTemplateSpecBuilder(List<BoxTestInfo> boxes,
                                                                         BoxAllocationMode boxAllocationMode) {
        var podAgentBuilder = createPodAgentBuilder();
        boxes.stream().map(boxAllocationMode::createBox).forEach(podAgentBuilder::addBoxes);

        var diskVolumeRequests = boxAllocationMode.getDiskVolumeRequests(boxes);

        return TPodTemplateSpec.newBuilder()
                .setSpec(DataModel.TPodSpec.newBuilder()
                        .addDiskVolumeRequests(DEFAULT_SIDECAR_VOLUME_REQUEST) //used for pod agent sidecar
                        .addAllDiskVolumeRequests(diskVolumeRequests)
                        .setPodAgentPayload(DataModel.TPodSpec.TPodAgentPayload.newBuilder().setSpec(podAgentBuilder))
                );
    }

    protected static TPodTemplateSpec.Builder createDefaultPodTemplateSpecBuilder() {
        return createPodTemplateSpecBuilder(DEFAULT_BOXES, BoxAllocationMode.WITHOUT_ALLOCATIONS);
    }

    @Test
    void jugglerDefaultBinaryIdGenerationTest() {
        assertThatEquals(JugglerPatcherV1Base.jugglerDefaultBinaryId(DEFAULT_DISK_ALLOCATION_ID), String.format("%s-%s",
                JUGGLER_DEFAULT_BINARY_ID, DEFAULT_DISK_ALLOCATION_ID));
    }

    @Test
    void jugglerBinaryIdGenerationTest() {
        assertThatEquals(JugglerPatcherV1Base.jugglerBinaryId(DEFAULT_DISK_ALLOCATION_ID, 0), String.format("juggler-binary" +
                        "-%s-%d",
                DEFAULT_DISK_ALLOCATION_ID, 0));
    }

    @Test
    void jugglerCheckIdGenerationTest() {
        assertThatEquals(JugglerPatcherV1Base.jugglerCheckId(DEFAULT_DISK_ALLOCATION_ID, 0), String.format("juggler-checks" +
                        "-%s-%d",
                DEFAULT_DISK_ALLOCATION_ID, 0));
    }

    @Test
    void jugglerWorkloadIdGenerationTest() {
        assertThatEquals(JugglerPatcherV1Base.jugglerWorkloadId(DEFAULT_BOX.getId()), String.format("%s-juggler-workload",
                DEFAULT_BOX.getId()));
    }

    protected TPodAgentSpec patchWithBoxesScenario(BoxAllocationMode boxAllocationMode) {
        return patchWithBoxesScenario(boxAllocationMode, box -> new BoxJugglerConfigBuilder());
    }

    private TPodAgentSpec patchWithBoxesScenario(BoxAllocationMode boxAllocationMode,
                                                 Function<BoxTestInfo, BoxJugglerConfigBuilder> boxConfigBuilderFactory) {
        var podTemplateSpecBuilder = createPodTemplateSpecBuilder(DEFAULT_BOXES, boxAllocationMode);

        var jugglerConfigBuilder = new JugglerConfigBuilder();
        DEFAULT_BOXES.forEach(box -> jugglerConfigBuilder.addBox(box, boxConfigBuilderFactory.apply(box)));

        return patch(DEFAULT_PATCHER_CONTEXT, podTemplateSpecBuilder, jugglerConfigBuilder).getPodAgentSpec();
    }

    @ParameterizedTest
    @EnumSource(BoxAllocationMode.class)
    void workloadsTest(BoxAllocationMode boxAllocationMode) {
        var podAgent = patchWithBoxesScenario(boxAllocationMode);
        DEFAULT_BOXES.forEach(box -> ensureBoxWorkloadIsPresent(podAgent, box));
    }

    @Test
    void resourcesMetaTest() {
        var podAgent = patchWithBoxesScenario(BoxAllocationMode.WITHOUT_ALLOCATIONS);

        var resources = DEFAULT_BOXES.stream()
                .map(box -> getBoxResource(podAgent, BoxAllocationMode.WITHOUT_ALLOCATIONS, DEFAULT_RESOURCE, box))
                .collect(Collectors.toUnmodifiableList());

        resources.forEach(resource -> {
            var expectedMeta = PodSpecUtils.resourceMeta(TestData.RESOURCE_META);
            assertThatEquals(resource.getMeta(), expectedMeta);
        });
    }

    @ParameterizedTest
    @EnumSource(BoxAllocationMode.class)
    void defaultResourceTest(BoxAllocationMode boxAllocationMode) {
        var podAgent = patchWithBoxesScenario(boxAllocationMode);

        DEFAULT_BOXES.forEach(box -> {
            var resource = getBoxResource(podAgent, boxAllocationMode, DEFAULT_RESOURCE, box);
            ensureBoxResourceIdIsCorrect(resource, JugglerPatcherV1Base::jugglerDefaultBinaryId);
            ensureBoxContainsResource(resource, box, podAgent);
        });
    }

    private void boxNonDefaultResourceIdScenario(BoxAllocationMode boxAllocationMode,
                                                 BiFunction<BoxJugglerConfigBuilder, DownloadableResource,
                                                         BoxJugglerConfigBuilder> boxConfigBuilderFactory,
                                                 BiFunction<String, Integer, String> expectedResourceIdFactory) {
        var podAgent = patchWithBoxesScenario(boxAllocationMode,
                box -> boxConfigBuilderFactory.apply(new BoxJugglerConfigBuilder(), box.getResource())
        );

        DEFAULT_BOXES.forEach(box -> {
            var resource = getBoxResource(podAgent, boxAllocationMode, box);
            ensureBoxResourceIdIsCorrect(resource, box, boxAllocationMode, expectedResourceIdFactory);
            ensureBoxContainsResource(resource, box, podAgent);
        });
    }

    @ParameterizedTest
    @EnumSource(BoxAllocationMode.class)
    void checksResourceTest(BoxAllocationMode boxAllocationMode) {
        boxNonDefaultResourceIdScenario(boxAllocationMode, BoxJugglerConfigBuilder::withArchivedCheck,
                JugglerPatcherV1Base::jugglerCheckId);
    }

    @ParameterizedTest
    @EnumSource(BoxAllocationMode.class)
    void binaryResourceTest(BoxAllocationMode boxAllocationMode) {
        boxNonDefaultResourceIdScenario(boxAllocationMode, BoxJugglerConfigBuilder::withJugglerBinary,
                JugglerPatcherV1Base::jugglerBinaryId);
    }

    private static Stream<Arguments> provideParametersForPortTest() {
        return Stream.of(
                Arguments.of(OptionalInt.of(137), 137),
                Arguments.of(OptionalInt.empty(), BoxJugglerConfig.DEFAULT_PORT)
        );
    }

    @ParameterizedTest
    @MethodSource("provideParametersForPortTest")
    public void portTest(OptionalInt initialPort, int expectedPort) {
        var podTemplateSpecBuilder = createDefaultPodTemplateSpecBuilder();

        var jugglerConfigBuilder = new JugglerConfigBuilder().addBox(DEFAULT_BOX,
                new BoxJugglerConfigBuilder().withPortInConfig(initialPort));

        var podSpec = patch(DEFAULT_PATCHER_CONTEXT, podTemplateSpecBuilder, jugglerConfigBuilder).getPodSpec();

        var expectedJugglerSubagents =
                ImmutableList.of(TMonitoringSubagentEndpoint.newBuilder().setPort(expectedPort).build());
        var actualJugglerSubagents = podSpec.getHostInfra().getMonitoring().getJugglerSubagentsList();

        assertThatEquals(actualJugglerSubagents, expectedJugglerSubagents);
    }

    protected static class ComputeResourcesTestUtils {
        private static final int NO_QUOTA = 0, ADD_QUOTA = 1;

         static long getQuotaPolicyCapacity(DataModel.TPodSpec.TDiskVolumeRequest volumeRequest) {
            return volumeRequest.getQuotaPolicy().getCapacity();
        }

        enum BoxesCountMode {
            ONE_BOX(ImmutableList.of(DEFAULT_BOX)), MANY_BOXES(DEFAULT_BOXES);

            private final List<BoxTestInfo> boxes;

            BoxesCountMode(List<BoxTestInfo> boxes) {
                this.boxes = boxes;
            }

            public List<BoxTestInfo> getBoxes() {
                return boxes;
            }
        }

        enum ExpectedDiskCapacityMode {
            JUGGLER_QUOTA_TO_SIDECAR(ADD_QUOTA, NO_QUOTA), JUGGLER_QUOTA_TO_USER(NO_QUOTA, ADD_QUOTA);

            private final int sidecarQuotaMultiplier, userQuotaMultiplier;

            ExpectedDiskCapacityMode(int sidecarQuotaMultiplier, int userQuotaMultiplier) {
                this.sidecarQuotaMultiplier = sidecarQuotaMultiplier;
                this.userQuotaMultiplier = userQuotaMultiplier;
            }

            public long getExpectedSidecarDiskCapacity(List<BoxTestInfo> boxes, long JUGGLER_QUOTA_PER_BOX) {
                long sidecarCapacity = getQuotaPolicyCapacity(DEFAULT_SIDECAR_VOLUME_REQUEST);
                sidecarCapacity += sidecarQuotaMultiplier * boxes.size() * JUGGLER_QUOTA_PER_BOX;
                return sidecarCapacity;
            }

            public long getExpectedUserDiskCapacity(BoxTestInfo boxTestInfo, long JUGGLER_QUOTA_PER_BOX) {
                long userCapacity = getQuotaPolicyCapacity(boxTestInfo.getVolumeRequest());
                userCapacity += userQuotaMultiplier * JUGGLER_QUOTA_PER_BOX;
                return userCapacity;
            }
        }
    }


    private static Stream<Arguments> provideParametersForComputeResourcesTest() {
        var streamBuilder = Stream.<Arguments>builder();

        for (var boxesMode : ComputeResourcesTestUtils.BoxesCountMode.values()) {
            streamBuilder.add(Arguments.of(boxesMode));
        }

        return streamBuilder.build();
    }

    @ParameterizedTest
    @MethodSource("provideParametersForComputeResourcesTest")
    public void computeResourcesTest(ComputeResourcesTestUtils.BoxesCountMode boxesCountMode) {

        List<BoxTestInfo> boxes = boxesCountMode.getBoxes();

        var patcherContext = DEFAULT_PATCHER_CONTEXT
                .withAllSidecarDiskAllocationIds(
                        ImmutableList.of(DEFAULT_SIDECAR_VOLUME_REQUEST.getId())
                );

        var podTemplateSpecBuilder = createPodTemplateSpecBuilder(boxes, BoxAllocationMode.WITH_ALLOCATIONS);

        var jugglerConfigBuilder = new JugglerConfigBuilder();
        boxes.forEach(jugglerConfigBuilder::addBox);

        var patchResult = patch(patcherContext, podTemplateSpecBuilder, jugglerConfigBuilder);
        var podSpec = patchResult.getPodSpec();

        var expectedBoxResources = getExpectedAdditionalBoxResources();

        var expectedSpecResources = expectedBoxResources.multiply(boxes.size());
        var actualSpecResources = podSpec.getResourceRequests();

        AssertUtils.assertResourceRequestEquals(actualSpecResources, expectedSpecResources);

        DataModel.TPodSpec.TDiskVolumeRequest sidecarDisk = podSpec.getDiskVolumeRequestsList().stream()
                .filter(r -> r.getId().equals(DEFAULT_SIDECAR_VOLUME_REQUEST.getId()))
                .findAny()
                .orElseThrow();

        var diskCapacityMode = getDiskCapacityMode();

        long expectedSidecarDiskCapacity = diskCapacityMode.getExpectedSidecarDiskCapacity(boxes, expectedBoxResources.getDiskCapacity());
        assertThatEquals(ComputeResourcesTestUtils.getQuotaPolicyCapacity(sidecarDisk), expectedSidecarDiskCapacity);

        boxes.forEach(boxTestInfo -> {
            TBox box = getBox(patchResult.getPodAgentSpec(), boxTestInfo);

            DataModel.TPodSpec.TDiskVolumeRequest userDisk = podSpec.getDiskVolumeRequestsList().stream()
                    .filter(r -> r.getId().equals(box.getVirtualDiskIdRef()))
                    .findAny().orElseThrow();

            long expectedUserDiskCapacity = diskCapacityMode.getExpectedUserDiskCapacity(boxTestInfo, expectedBoxResources.getDiskCapacity());
            assertThatEquals(ComputeResourcesTestUtils.getQuotaPolicyCapacity(userDisk), expectedUserDiskCapacity);
            var actualComputeResources = box.getComputeResources();
            var expectedBoxGuarantee = getExpectedBoxGuaranteeResources(box);
            assertThatEquals(
                    actualComputeResources.getMemoryGuarantee(),
                    expectedBoxGuarantee.getMemoryGuarantee()
            );
        });
    }

    protected abstract ComputeResourcesTestUtils.ExpectedDiskCapacityMode getDiskCapacityMode();

    @ParameterizedTest
    @EnumSource(
            value = Enums.EPodHostNameKind.class,
            names = {"UNRECOGNIZED"},
            mode = EnumSource.Mode.EXCLUDE
    )
    void setUserHostNameKindTest(Enums.EPodHostNameKind initialHostNameKind) {
        var podTemplateSpecBuilder = createDefaultPodTemplateSpecBuilder();
        podTemplateSpecBuilder.getSpecBuilder().setHostNameKind(initialHostNameKind);
        setHostNameKindScenario(podTemplateSpecBuilder, initialHostNameKind);
    }

    protected void setHostNameKindScenario(TPodTemplateSpec.Builder podTemplateSpecBuilder,
                                           Enums.EPodHostNameKind expectedHostNameKind) {
        var jugglerConfigBuilder = new JugglerConfigBuilder().addBox(DEFAULT_BOX);

        var podSpec = patch(DEFAULT_PATCHER_CONTEXT, podTemplateSpecBuilder, jugglerConfigBuilder).getPodSpec();

        var actualHostNameKind = podSpec.getHostNameKind();
        assertThatEquals(actualHostNameKind, expectedHostNameKind);
    }

    private PatchResult patch(JugglerPatcherV1Context patcherContext,
                                     TPodTemplateSpec.Builder podTemplateSpecBuilder,
                                     JugglerConfigBuilder jugglerConfigBuilder) {
        var deployUnitContext = TestData.DEFAULT_UNIT_CONTEXT.toBuilder()
                .withSpec(DeployUnitSpec::withJugglerConfig, jugglerConfigBuilder.build())
                .withResolvedSbr(TestData.SANDBOX_RESOLVED_RESOURCES)
                .build();
        return patch(patcherContext, podTemplateSpecBuilder, deployUnitContext);
    }

    private static TResource getBoxResource(TPodAgentSpec podAgentSpec, BoxAllocationMode boxAllocationMode,
                                            BoxTestInfo box) {
        var expectedBoxResultResource = box.getExpectedResource();
        return getBoxResource(podAgentSpec, boxAllocationMode, expectedBoxResultResource, box);
    }

    private static TResource getBoxResource(TPodAgentSpec podAgentSpec, BoxAllocationMode boxAllocationMode,
                                            DownloadableResource expectedBoxResultResource, BoxTestInfo box) {
        var expectedBoxResultAllocationId = boxAllocationMode.getBoxResultAllocationId(box);

        return podAgentSpec.getResources().getStaticResourcesList().stream()
                .filter(current -> isExpectedResource(current, expectedBoxResultResource,
                        expectedBoxResultAllocationId))
                .findAny().orElseThrow();
    }

    private static boolean isExpectedResource(TResource current, DownloadableResource expectedBoxResource,
                                              String expectedBoxAllocationId) {
        return current.getUrl().equals(expectedBoxResource.getUrl()) &&
                current.getVerification().getChecksum().equals(expectedBoxResource.getChecksum().toAgentFormat()) &&
                current.getVirtualDiskIdRef().equals(expectedBoxAllocationId);
    }

    private static TBox getBox(TPodAgentSpec podAgentSpec, BoxTestInfo boxTestInfo) {
        return PatcherTestUtils.getBoxById(podAgentSpec, boxTestInfo.getId()).orElseThrow();
    }

    private static void ensureBoxResourceIdIsCorrect(TResource resource,
                                                     Function<String, String> expectedResourceIdFactory) {
        String expectedResourceId = expectedResourceIdFactory.apply(resource.getVirtualDiskIdRef());
        assertThatEquals(resource.getId(), expectedResourceId);
    }

    private static void ensureBoxResourceIdIsCorrect(TResource resource, BoxTestInfo boxTestInfo,
                                                     BoxAllocationMode boxAllocationMode,
                                                     BiFunction<String, Integer, String> expectedResourceIdFactory) {
        int expectedResourceIndex = boxAllocationMode.getBoxResultResourceIndex(boxTestInfo);
        ensureBoxResourceIdIsCorrect(resource, (resourceId) -> expectedResourceIdFactory.apply(resourceId,
                expectedResourceIndex));
    }

    private static void ensureBoxContainsResource(TResource resource, BoxTestInfo boxTestInfo,
                                                  TPodAgentSpec podAgentSpec) {
        TBox box = getBox(podAgentSpec, boxTestInfo);

        var boxStaticResources = box.getStaticResourcesList().stream()
                .map(TMountedStaticResource::getResourceRef)
                .collect(Collectors.toList());

        assertThat(boxStaticResources, hasItem(resource.getId()));
    }

    protected static Optional<TWorkload> getJugglerWorkload(TPodAgentSpec podAgent, BoxTestInfo boxInfo) {
        String expectedId = JugglerPatcherV1Base.jugglerWorkloadId(boxInfo.id);

        return podAgent.getWorkloadsList().stream()
                .filter(w -> w.getId().equals(expectedId))
                .filter(w -> w.getBoxRef().equals(boxInfo.id))
                .findAny();
    }

    protected static void ensureBoxWorkloadIsPresent(TPodAgentSpec podAgent, BoxTestInfo boxInfo) {
        var workload = getJugglerWorkload(podAgent, boxInfo);
        assertThatEquals(workload.isPresent(), true);
    }

    protected void ensureWorkloadsComputedResources(TPodAgentSpec podAgentSpec, TComputeResources resources) {
        DEFAULT_BOXES.stream()
                .map(box -> getJugglerWorkload(podAgentSpec, box))
                .map(Optional::get)
                .map(workload -> workload.getStart().getComputeResources())
                .forEach(computeResources -> {
                    assertThatEquals(computeResources.getMemoryGuarantee(), resources.getMemoryGuarantee());
                    assertThatEquals(computeResources.getMemoryLimit(), resources.getMemoryLimit());
                    assertThatEquals(computeResources.getVcpuGuarantee(), resources.getVcpuGuarantee());
                    assertThatEquals(computeResources.getVcpuLimit(), resources.getVcpuLimit());
                });
    }

    protected void ensureJugglerWorkloadsStartCommand(TPodAgentSpec podAgentSpec, String expectedStartCmd) {
       DEFAULT_BOXES.stream()
                .map(box -> getJugglerWorkload(podAgentSpec, box))
                .map(Optional::get)
                .map(workload -> workload.getStart().getCommandLine())
                .forEach(startCommand -> assertThatEquals(startCommand, expectedStartCmd));
    }

    private static Optional<TBox> getBoxWithJuggler(TPodAgentSpec podAgent, BoxTestInfo boxInfo) {
        return podAgent.getBoxesList().stream()
                .filter(b -> b.getId().equals(boxInfo.id))
                .findAny();
    }

    protected void ensureBoxesWithJugglerInitCommand(TPodAgentSpec podAgentSpec, String expectedInitCmd) {
        DEFAULT_BOXES.stream()
                .map(b -> getBoxWithJuggler(podAgentSpec, b))
                .map(Optional::get)
                .map(b -> b.getInit(0).getCommandLine())
                .forEach(initCmd -> assertThatEquals(initCmd, expectedInitCmd));
    }

    private static class JugglerConfigBuilder {

        private final ImmutableMap.Builder<String, BoxJugglerConfig> jugglerConfigBuilder;

        JugglerConfigBuilder() {
            this.jugglerConfigBuilder = ImmutableMap.builder();
        }

        JugglerConfigBuilder addBox(BoxTestInfo boxTestInfo) {
            return addBox(boxTestInfo, new BoxJugglerConfigBuilder());
        }

        JugglerConfigBuilder addBox(BoxTestInfo boxTestInfo, BoxJugglerConfigBuilder boxJugglerConfigBuilder) {
            jugglerConfigBuilder.put(boxTestInfo.id, boxJugglerConfigBuilder.build());
            return this;
        }

        Map<String, BoxJugglerConfig> build() {
            return jugglerConfigBuilder.build();
        }
    }

    private static class BoxJugglerConfigBuilder {

        private final ImmutableList.Builder<DownloadableResource> archivedChecksBuilder;
        private OptionalInt portInConfig;
        private DownloadableResource jugglerBinary;

        BoxJugglerConfigBuilder() {
            this.archivedChecksBuilder = ImmutableList.builder();
            this.portInConfig = OptionalInt.empty();
            this.jugglerBinary = null;
        }

        BoxJugglerConfigBuilder withArchivedCheck(DownloadableResource resource) {
            archivedChecksBuilder.add(resource);
            return this;
        }

        BoxJugglerConfigBuilder withPortInConfig(OptionalInt portInConfig) {
            this.portInConfig = portInConfig;
            return this;
        }

        BoxJugglerConfigBuilder withJugglerBinary(DownloadableResource jugglerBinary) {
            this.jugglerBinary = jugglerBinary;
            return this;
        }

        BoxJugglerConfig build() {
            Optional<DownloadableResource> jugglerBinaryOptional = Optional.ofNullable(jugglerBinary);
            return new BoxJugglerConfig(archivedChecksBuilder.build(), portInConfig,
                    jugglerBinaryOptional);
        }
    }
}

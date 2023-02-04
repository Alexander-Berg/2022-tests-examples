package ru.yandex.infra.stage.podspecs.patcher.defaults;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.deployunit.DeployUnitContext;
import ru.yandex.infra.stage.dto.DeployUnitSpec;
import ru.yandex.infra.stage.dto.DownloadableResource;
import ru.yandex.infra.stage.dto.NetworkDefaults;
import ru.yandex.infra.stage.dto.PodAgentConfig;
import ru.yandex.infra.stage.dto.SidecarVolumeSettings;
import ru.yandex.infra.stage.podspecs.FixedResourceSupplier;
import ru.yandex.infra.stage.podspecs.PatcherTestUtils;
import ru.yandex.infra.stage.podspecs.PodSpecUtils;
import ru.yandex.infra.stage.podspecs.ResourceWithMeta;
import ru.yandex.infra.stage.podspecs.SidecarDiskVolumeDescription;
import ru.yandex.infra.stage.podspecs.patcher.PatcherTestBase;
import ru.yandex.infra.stage.util.NamedArgument;
import ru.yandex.infra.stage.util.OptionalUtils;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree;
import ru.yandex.yp.client.api.DataModel;
import ru.yandex.yp.client.api.Enums;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.pods.EResolvConf;
import ru.yandex.yp.client.pods.TBox;
import ru.yandex.yp.client.pods.TLayer;
import ru.yandex.yp.client.pods.TMutableWorkload;
import ru.yandex.yp.client.pods.TPodAgentSpec;
import ru.yandex.yp.client.pods.TResource;
import ru.yandex.yp.client.pods.TResourceGang;
import ru.yandex.yp.client.pods.TVolume;
import ru.yandex.yp.client.pods.TWorkload;
import ru.yandex.yt.ytree.TAttribute;
import ru.yandex.yt.ytree.TAttributeDictionary;

import static com.spotify.hamcrest.optional.OptionalMatchers.emptyOptional;
import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.in;
import static ru.yandex.infra.stage.TestData.DEFAULT_UNIT_CONTEXT;
import static ru.yandex.infra.stage.TestData.NETWORK_DEFAULTS;
import static ru.yandex.infra.stage.TestData.NETWORK_ID;
import static ru.yandex.infra.stage.TestData.REPLICA_SET_UNIT_SPEC;
import static ru.yandex.infra.stage.TestData.RESOURCE_ID;
import static ru.yandex.infra.stage.TestData.USED_BY_INFRA_VOLUME_REQUEST;
import static ru.yandex.infra.stage.podspecs.PatcherTestUtils.testIds;
import static ru.yandex.infra.stage.podspecs.PodSpecUtils.DEFAULT_BOX_SPECIFIC_TYPE;
import static ru.yandex.infra.stage.podspecs.PodSpecUtils.DISABLE_DISK_ISOLATION_LABEL_ATTRIBUTE;
import static ru.yandex.infra.stage.podspecs.PodSpecUtils.USED_BY_INFRA_LABEL;
import static ru.yandex.infra.stage.podspecs.patcher.defaults.DefaultsPatcherV1Base.SOX_FILTER;
import static ru.yandex.infra.stage.util.AssertUtils.assertCollectionMatched;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

public abstract class DefaultsPatcherV1BaseTest extends PatcherTestBase<DefaultsPatcherV1Context> {
    private static final ResourceWithMeta POD_AGENT_BINARY_SUPPLIER =
            new ResourceWithMeta(TestData.DOWNLOADABLE_RESOURCE, Optional.of(TestData.RESOURCE_META));

    private static final ResourceWithMeta POD_AGENT_BASE_LAYER_SUPPLIER =
            new ResourceWithMeta(TestData.DOWNLOADABLE_RESOURCE2, Optional.of(TestData.RESOURCE_META));

    public static final String DEFAULT_BOX_ID = "box_id";
    public static final String DEFAULT_POD_AGENT_ALLOCATION_ID = "pod_agent_allocation_id";

    private static final boolean DEFAULT_USE_PATCH_BOX_SPECIFIC_TYPE = false;

    private static final long DEFAULT_RELEASE_GETTER_TIMEOUT_SECONDS = 5;

    private static final boolean DEFAULT_PLACE_BINARY_REVISION_TO_POD_AGENT_META = false;

    public static final DefaultsPatcherV1Context DEFAULT_PATCHER_CONTEXT = new DefaultsPatcherV1Context(
            FixedResourceSupplier.withMeta(POD_AGENT_BINARY_SUPPLIER.getResource(), TestData.RESOURCE_META),
            FixedResourceSupplier.withMeta(POD_AGENT_BASE_LAYER_SUPPLIER.getResource(), TestData.RESOURCE_META),
            Optional.of(DEFAULT_POD_AGENT_ALLOCATION_ID),
            DEFAULT_USE_PATCH_BOX_SPECIFIC_TYPE,
            DEFAULT_RELEASE_GETTER_TIMEOUT_SECONDS,
            DEFAULT_PLACE_BINARY_REVISION_TO_POD_AGENT_META,
            Optional.of(ImmutableList.of(DEFAULT_POD_AGENT_ALLOCATION_ID))
    );

    public static final String CHANGED_NETWORK_ID = NETWORK_ID + "1";

    private static final String DEFAULT_NOT_SOX_FILTER_LABEL = "[/labels/cpu_flags/avx2] = true";

    private static Stream<Arguments> addPodAgentDiskTestParameters() {
        return Stream.of(
                Arguments.of(
                        NamedArgument.of("empty", Optional.empty()),
                        USED_BY_INFRA_VOLUME_REQUEST.getStorageClass()
                ),
                Arguments.of(
                        NamedArgument.of(
                                "auto",
                                Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.AUTO))
                        ),
                        USED_BY_INFRA_VOLUME_REQUEST.getStorageClass()
                ),
                Arguments.of(
                        NamedArgument.of(
                                "hdd",
                                Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.HDD))
                        ),
                        SidecarDiskVolumeDescription.HDD_STORAGE_CLASS
                ),
                Arguments.of(
                        NamedArgument.of(
                                "ssd",
                                Optional.of(new SidecarVolumeSettings(SidecarVolumeSettings.StorageClass.SSD))
                        ),
                        SidecarDiskVolumeDescription.SSD_STORAGE_CLASS
                )
        );
    }

    @ParameterizedTest
    @MethodSource("addPodAgentDiskTestParameters")
    void addPodAgentDiskTest(
            NamedArgument<Optional<SidecarVolumeSettings>> namedPodAgentVolumeSettings,
            String expectedPodAgentDiskStorageClass
    ) {
        TPodTemplateSpec.Builder podTemplateSpecBuilder = createDefaultPodTemplateSpecBuilder();

        var deployUnitContext = DEFAULT_UNIT_CONTEXT.withSpecDetails(
                REPLICA_SET_UNIT_SPEC.withPodAgentConfigExtractor(
                        podAgentDeploymentMeta -> new PodAgentConfig(namedPodAgentVolumeSettings.getArgument())
                )
        );

        var patchResult = patch(
                DEFAULT_PATCHER_CONTEXT,
                podTemplateSpecBuilder,
                deployUnitContext
        );

        DataModel.TPodSpec spec = patchResult.getPodSpec();
        String expectedRootAllocationId = USED_BY_INFRA_VOLUME_REQUEST.getId();

        checkDiskRequestExists(
                spec,
                expectedRootAllocationId,
                Optional.empty(),
                USED_BY_INFRA_VOLUME_REQUEST.getStorageClass()
        );
        checkDiskRequestExists(spec, DEFAULT_POD_AGENT_ALLOCATION_ID, Optional.of(USED_BY_INFRA_LABEL),
                expectedPodAgentDiskStorageClass);

        TBox rootBox = PatcherTestUtils.getBoxById(
                patchResult.getPodAgentSpec(),
                DefaultsPatcherV1Base.ROOT_BOX_ID
        ).orElseThrow();

        Optional<String> actualRootAllocationId = OptionalUtils.emptyStringAsEmptyOptional(
                rootBox.getVirtualDiskIdRef()
        );

        assertThatEquals(actualRootAllocationId.orElseThrow(), expectedRootAllocationId);
    }

    private static Stream<Arguments> notAddPodAgentDiskVolumeRequestTestParameters() {
        return Stream.of(
                Arguments.of(
                        Optional.of(ImmutableList.of(DEFAULT_POD_AGENT_ALLOCATION_ID)),
                        Optional.of(DEFAULT_POD_AGENT_ALLOCATION_ID),
                        List.of(DISABLE_DISK_ISOLATION_LABEL_ATTRIBUTE)
                ),
                Arguments.of(Optional.empty(), Optional.empty(), List.of())
        );
    }

    @ParameterizedTest
    @MethodSource("notAddPodAgentDiskVolumeRequestTestParameters")
    void notAddPodAgentDiskVolumeRequestTest(
            Optional<List<String>> allSidecarAllocationIds,
            Optional<String> podAgentAllocationId,
            Iterable<TAttribute> attributes
    ) {
        TPodTemplateSpec.Builder podTemplateSpecBuilder = createDefaultPodTemplateSpecBuilder().setLabels(
                TAttributeDictionary.newBuilder().addAllAttributes(attributes).build()
        );

        var patcherContext = DEFAULT_PATCHER_CONTEXT.toBuilder()
                .withAllSidecarDiskAllocationIds(allSidecarAllocationIds)
                .withPodAgentAllocationId(podAgentAllocationId)
                .build();

        var patchResult = patch(patcherContext, podTemplateSpecBuilder, DEFAULT_UNIT_CONTEXT);

        checkDiskRequestExists(
                patchResult.getPodSpec(),
                USED_BY_INFRA_VOLUME_REQUEST.getId(),
                Optional.of(USED_BY_INFRA_LABEL),
                USED_BY_INFRA_VOLUME_REQUEST.getStorageClass()
        );
        assertThatEquals(patchResult.getPodSpec().getDiskVolumeRequestsList().size(), 1);

        TBox rootBox = PatcherTestUtils.getBoxById(
                patchResult.getPodAgentSpec(),
                DefaultsPatcherV1Base.ROOT_BOX_ID
        ).orElseThrow();

        Optional<String> actualRootDiskId = OptionalUtils.emptyStringAsEmptyOptional(rootBox.getVirtualDiskIdRef());

        assertThat(actualRootDiskId, emptyOptional());
    }

    private static Stream<Arguments> patchEmptyVirtualDisksRefsTestParameters() {
        return Stream.of(
                Arguments.of(
                        NamedArgument.of(
                                "single volume request",
                                ImmutableList.of(USED_BY_INFRA_VOLUME_REQUEST.toBuilder())
                        ),
                        equalTo(USED_BY_INFRA_VOLUME_REQUEST.getId())
                ),
                Arguments.of(
                        NamedArgument.of(
                                "many volume requests",
                                ImmutableList.of(
                                        createDiskVolumeRequestBuilder(
                                                "first_allocation_id", Optional.of(USED_BY_INFRA_LABEL)),
                                        createDiskVolumeRequestBuilder(
                                                "second_allocation_id", Optional.empty()))
                        ),
                        Matchers.emptyString()
                )
        );
    }

    @ParameterizedTest
    @MethodSource("patchEmptyVirtualDisksRefsTestParameters")
    void patchEmptyVirtualDisksRefsTest(
            NamedArgument<List<DataModel.TPodSpec.TDiskVolumeRequest.Builder>> namedAllocationsExceptPodAgent,
            Matcher<String> matcher
    ) {
        TPodTemplateSpec.Builder podTemplateSpecBuilder = createDefaultPodTemplateSpecBuilder();

        TPodAgentSpec.Builder podAgentSpecBuilder = createPodAgentSpecBuilder();
        DataModel.TPodSpec.Builder podSpecBuilder = podTemplateSpecBuilder.getSpecBuilder();
        podSpecBuilder.getPodAgentPayloadBuilder().setSpec(podAgentSpecBuilder);

        podSpecBuilder.clearDiskVolumeRequests();
        namedAllocationsExceptPodAgent.getArgument().forEach(podSpecBuilder::addDiskVolumeRequests);

        var patchResult = patch(
                DEFAULT_PATCHER_CONTEXT,
                podTemplateSpecBuilder,
                DEFAULT_UNIT_CONTEXT
        );
        TPodAgentSpec podAgentSpec = patchResult.getPodAgentSpec();

        assertThat(podAgentSpec.getVolumes(0).getVirtualDiskIdRef(), matcher);
        assertThat(podAgentSpec.getBoxes(0).getVirtualDiskIdRef(), matcher);
        assertThat(podAgentSpec.getResources().getLayers(0).getVirtualDiskIdRef(), matcher);
        assertThat(podAgentSpec.getResources().getStaticResources(0).getVirtualDiskIdRef(), matcher);
    }

    @Test
    void patchBoxSpecificTypeTest() {
        TPodAgentSpec.Builder podAgentSpecBuilder = createPodAgentSpecBuilder();
        String newBoxId = "new_box";
        String newBoxType = "type";
        podAgentSpecBuilder.addBoxes(TBox.newBuilder()
                .setId(newBoxId)
                .setSpecificType(newBoxType)
                .build());

        TPodTemplateSpec.Builder podTemplateSpecBuilder = createDefaultPodTemplateSpecBuilder();

        podTemplateSpecBuilder.getSpecBuilder().getPodAgentPayloadBuilder().setSpec(podAgentSpecBuilder);

        var patchResult = patch(
                DEFAULT_PATCHER_CONTEXT.withPatchBoxSpecificType(true),
                podTemplateSpecBuilder,
                DEFAULT_UNIT_CONTEXT
        );

        assertThat(PatcherTestUtils.getBoxById(patchResult.getPodAgentSpec(), DEFAULT_BOX_ID)
                .orElseThrow().getSpecificType(), equalTo(DEFAULT_BOX_SPECIFIC_TYPE));
        assertThat(PatcherTestUtils.getBoxById(patchResult.getPodAgentSpec(), newBoxId)
                .orElseThrow().getSpecificType(), equalTo(newBoxType));
    }

    DataModel.TPodSpec.TPodAgentDeploymentMeta patchWithPlaceBinaryRevisionToPodAgentMetaAndGetMeta(
            DataModel.TPodSpec.TPodAgentPayload.Builder agentPayloadBuilder,
            boolean placeBinaryRevisionToPodAgentMeta
    ) {
        TPodTemplateSpec.Builder podTemplateSpecBuilder = createDefaultPodTemplateSpecBuilder();
        podTemplateSpecBuilder.getSpecBuilder().setPodAgentPayload(agentPayloadBuilder);

        var patcherContext = DEFAULT_PATCHER_CONTEXT.withPlaceBinaryRevisionToPodAgentMeta(
                placeBinaryRevisionToPodAgentMeta
        );
        var patchResult = patch(patcherContext, podTemplateSpecBuilder, DEFAULT_UNIT_CONTEXT);

        return patchResult.getPodAgentPayload().getMeta();
    }

    @ParameterizedTest
    @CsvSource({
            "true, " + RESOURCE_ID,
            "false, 0"
    })
    void addPodAgentMetaIfNoMetaInSpecTest(
            boolean placeBinaryRevisionToPodAgentMeta,
            Long expectedPodAgentBinaryRevisionInMeta
    ) {
        DataModel.TPodSpec.TPodAgentDeploymentMeta agentMeta = patchWithPlaceBinaryRevisionToPodAgentMetaAndGetMeta(
                DataModel.TPodSpec.TPodAgentPayload.newBuilder(),
                placeBinaryRevisionToPodAgentMeta
        );

        assertThat(agentMeta.getUrl(), equalTo(TestData.DOWNLOADABLE_RESOURCE.getUrl()));
        assertThat(agentMeta.getChecksum(), equalTo(TestData.DOWNLOADABLE_RESOURCE.getChecksum().toAgentFormat()));
        assertThat(agentMeta.getBinaryRevision(), equalTo(expectedPodAgentBinaryRevisionInMeta));
        assertThat(agentMeta.getLayersCount(), equalTo(1));

        DataModel.TPodSpec.TPodAgentDeploymentMeta.TLayer podAgentBaseLayer = agentMeta.getLayers(0);
        assertThat(podAgentBaseLayer.getUrl(), equalTo(TestData.DOWNLOADABLE_RESOURCE2.getUrl()));
        assertThat(podAgentBaseLayer.getChecksum(),
                equalTo(TestData.DOWNLOADABLE_RESOURCE2.getChecksum().toAgentFormat()));
    }

    @ParameterizedTest
    @CsvSource({
            "true, " + RESOURCE_ID,
            "false, 0"
    })
    void addPodAgentBinaryToMetaIfNoBinaryInSpecTest(
            boolean placeBinaryRevisionToPodAgentMeta,
            Long expectedPodAgentBinaryRevisionInMeta
    ) {
        DownloadableResource podAgentBaseLayer = POD_AGENT_BASE_LAYER_SUPPLIER.getResource();

        DataModel.TPodSpec.TPodAgentPayload.Builder agentPayloadBuilder =
                DataModel.TPodSpec.TPodAgentPayload.newBuilder()
                        .setMeta(DataModel.TPodSpec.TPodAgentDeploymentMeta.newBuilder()
                                .addLayers(DataModel.TPodSpec.TPodAgentDeploymentMeta.TLayer.newBuilder()
                                        .setUrl(podAgentBaseLayer.getUrl())
                                        .setChecksum(podAgentBaseLayer.getChecksum().toAgentFormat())));

        DataModel.TPodSpec.TPodAgentDeploymentMeta agentMeta = patchWithPlaceBinaryRevisionToPodAgentMetaAndGetMeta(
                agentPayloadBuilder,
                placeBinaryRevisionToPodAgentMeta
        );

        assertThat(agentMeta.getUrl(), equalTo(TestData.DOWNLOADABLE_RESOURCE.getUrl()));
        assertThat(agentMeta.getChecksum(), equalTo(TestData.DOWNLOADABLE_RESOURCE.getChecksum().toAgentFormat()));
        assertThat(agentMeta.getBinaryRevision(), equalTo(expectedPodAgentBinaryRevisionInMeta));
    }

    @ParameterizedTest
    @CsvSource({
            "true",
            "false"
    })
    void addPodAgentBaseLayerToMetaIfNoBaseLayerInSpecTest(boolean placeBinaryRevisionToPodAgentMeta) {
        DownloadableResource podAgentBinary = POD_AGENT_BINARY_SUPPLIER.getResource();

        DataModel.TPodSpec.TPodAgentPayload.Builder agentPayloadBuilder =
                DataModel.TPodSpec.TPodAgentPayload.newBuilder()
                        .setMeta(DataModel.TPodSpec.TPodAgentDeploymentMeta.newBuilder()
                                .setUrl(podAgentBinary.getUrl())
                                .setChecksum(podAgentBinary.getChecksum().toAgentFormat()));

        DataModel.TPodSpec.TPodAgentDeploymentMeta agentMeta = patchWithPlaceBinaryRevisionToPodAgentMetaAndGetMeta(
                agentPayloadBuilder,
                placeBinaryRevisionToPodAgentMeta
        );

        // Pod agent binary revision is not set with custom pod agent binary
        assertThat(agentMeta.getBinaryRevision(), equalTo(0L));
        assertThat(agentMeta.getLayersCount(), equalTo(1));

        DataModel.TPodSpec.TPodAgentDeploymentMeta.TLayer podAgentBaseLayer = agentMeta.getLayers(0);
        assertThat(podAgentBaseLayer.getUrl(), equalTo(TestData.DOWNLOADABLE_RESOURCE2.getUrl()));
        assertThat(podAgentBaseLayer.getChecksum(),
                equalTo(TestData.DOWNLOADABLE_RESOURCE2.getChecksum().toAgentFormat()));
    }

    private void patchNetworksDefaultsScenario(
            TPodTemplateSpec.Builder podTemplateSpec,
            NetworkDefaults networkDefaults,
            int expectedIp6AddressRequestsSize,
            String expectedIp6AddressNetworkId,
            String expectedIp6SubnetNetworkId
    ) {
        var deployUnitContext = DEFAULT_UNIT_CONTEXT.withNetworkDefaults(networkDefaults);

        var patchResult = patch(
                DEFAULT_PATCHER_CONTEXT,
                podTemplateSpec,
                deployUnitContext
        );

        DataModel.TPodSpec podSpec = patchResult.getPodSpec();

        assertCollectionMatched(
                podSpec.getIp6AddressRequestsList(),
                expectedIp6AddressRequestsSize,
                req -> req.getNetworkId().equals(expectedIp6AddressNetworkId)
        );
        assertCollectionMatched(
                podSpec.getIp6SubnetRequestsList(),
                1,
                req -> req.getNetworkId().equals(expectedIp6SubnetNetworkId)
        );
    }

    @Test
    void patchNetworksDefaults() {
        TPodTemplateSpec.Builder podTemplateSpecBuilder = createDefaultPodTemplateSpecBuilder();

        patchNetworksDefaultsScenario(
                podTemplateSpecBuilder,
                NETWORK_DEFAULTS,
                2,
                TestData.NETWORK_DEFAULTS.getNetworkId(),
                TestData.NETWORK_DEFAULTS.getNetworkId()
        );
    }

    @ParameterizedTest
    @CsvSource({
            "false, 1, " + CHANGED_NETWORK_ID,
            "true, 2, " + NETWORK_ID
    })
    void patchNetworksAddressOverrideTest(
            boolean addressOverride,
            int expectedIp6AddressRequestsSize,
            String expectedIp6AddressNetworkId
    ) {
        NetworkDefaults networkDefaults = NETWORK_DEFAULTS.withOverrideIp6AddressRequests(addressOverride);

        TPodTemplateSpec.Builder podTemplateSpecBuilder = createDefaultPodTemplateSpecBuilder();
        podTemplateSpecBuilder.getSpecBuilder()
                .setPodAgentPayload(
                        DataModel.TPodSpec.TPodAgentPayload.newBuilder().setSpec(createPodAgentSpecBuilder())
                )
                .addIp6AddressRequests(
                        DataModel.TPodSpec.TIP6AddressRequest.newBuilder()
                                .setNetworkId(CHANGED_NETWORK_ID)
                                .setVlanId("someVlanId")
                );

        patchNetworksDefaultsScenario(
                podTemplateSpecBuilder,
                networkDefaults,
                expectedIp6AddressRequestsSize,
                expectedIp6AddressNetworkId,
                networkDefaults.getNetworkId()
        );
    }

    @Test
    void patchNetworksDefaultsWithIp4AddressPool() {
        String poolId = "pool_id";

        TPodTemplateSpec.Builder podTemplateSpecBuilder = createDefaultPodTemplateSpecBuilder();

        NetworkDefaults networkDefaults = NETWORK_DEFAULTS.withIp4AddressPoolId(poolId);
        var deployUnitContext = DEFAULT_UNIT_CONTEXT.withNetworkDefaults(networkDefaults);

        var patchResult = patch(
                DEFAULT_PATCHER_CONTEXT,
                podTemplateSpecBuilder,
                deployUnitContext
        );

        DataModel.TPodSpec podSpec = patchResult.getPodSpec();

        assertCollectionMatched(
                podSpec.getIp6AddressRequestsList(),
                2,
                req -> req.getIp4AddressPoolId().equals(poolId)
        );
    }

    protected void patchSoxLabelsScenario(boolean isSoxService, String nodeFilter, String expectedNodeFilter) {
        TPodTemplateSpec.Builder podTemplateSpecBuilder = createDefaultPodTemplateSpecBuilder();
        podTemplateSpecBuilder.getSpecBuilder().setNodeFilter(nodeFilter);

        var deployUnitContext = DEFAULT_UNIT_CONTEXT.withDeployUnitSpec(
                DeployUnitSpec::withSoxService, isSoxService
        );

        var patchResult = patch(
                DEFAULT_PATCHER_CONTEXT,
                podTemplateSpecBuilder,
                deployUnitContext
        );

        assertThatEquals(
                patchResult.getPodSpec().getNodeFilter(),
                expectedNodeFilter
        );
    }

    private static Stream<Arguments> patchSoxLabelsTestParametersGenerator(
            String expectedFilterForSoxServiceWithNotSoxFilterLabel
    ) {
        return Stream.of(
                Arguments.of(false, "", ""),
                Arguments.of(false, DEFAULT_NOT_SOX_FILTER_LABEL, DEFAULT_NOT_SOX_FILTER_LABEL),
                Arguments.of(true, "", SOX_FILTER),
                Arguments.of(true, SOX_FILTER, SOX_FILTER),
                Arguments.of(true, DEFAULT_NOT_SOX_FILTER_LABEL, expectedFilterForSoxServiceWithNotSoxFilterLabel)
        );
    }

    protected static Stream<Arguments> patchSoxLabelsTestParametersFromV1ToV3() {
        return patchSoxLabelsTestParametersGenerator(SOX_FILTER);
    }

    protected static Stream<Arguments> patchSoxLabelsTestParametersFromV4ToLast() {
        return patchSoxLabelsTestParametersGenerator(
                String.format("%s AND %s", SOX_FILTER, DEFAULT_NOT_SOX_FILTER_LABEL)
        );
    }

    @Test
    void patchNetworksDefaultsWithVirtualServiceIds() {
        List<String> virtualServiceIds = ImmutableList.of("s1", "s2");

        TPodTemplateSpec.Builder podTemplateSpecBuilder = createDefaultPodTemplateSpecBuilder();

        NetworkDefaults networkDefaults = NETWORK_DEFAULTS.withVirtualServiceIds(virtualServiceIds);
        var deployUnitContext = DEFAULT_UNIT_CONTEXT.withNetworkDefaults(networkDefaults);

        var patchResult = patch(
                DEFAULT_PATCHER_CONTEXT,
                podTemplateSpecBuilder,
                deployUnitContext
        );

        DataModel.TPodSpec podSpec = patchResult.getPodSpec();

        assertCollectionMatched(
                podSpec.getIp6AddressRequestsList(),
                2,
                req -> req.getVirtualServiceIdsList().containsAll(virtualServiceIds)
        );
    }

    @Test
    void addDefaultBoxAndMutableWorkloadsTest() {
        String workloadId = "workload_id";

        TPodAgentSpec.Builder podAgentSpecBuilder = TPodAgentSpec.newBuilder()
                .addWorkloads(TWorkload.newBuilder()
                        .setId(workloadId)
                        .build());

        TPodTemplateSpec.Builder podTemplateSpecBuilder = createDefaultPodTemplateSpecBuilder();

        podTemplateSpecBuilder.getSpecBuilder().getPodAgentPayloadBuilder().setSpec(podAgentSpecBuilder);

        var patchResult = patch(
                DEFAULT_PATCHER_CONTEXT,
                podTemplateSpecBuilder,
                DEFAULT_UNIT_CONTEXT
        );
        TPodAgentSpec podAgentSpec = patchResult.getPodSpec().getPodAgentPayload().getSpec();

        List<TBox> boxes = podAgentSpec.getBoxesList();
        testIds(boxes, ImmutableSet.of(DefaultsPatcherV1Base.ROOT_BOX_ID), TBox::getId);

        TBox rootBox = boxes.stream()
                .filter(b -> b.getId().equals(DefaultsPatcherV1Base.ROOT_BOX_ID))
                .findFirst().orElseThrow();

        assertThat(rootBox.getVirtualDiskIdRef(), equalTo(USED_BY_INFRA_VOLUME_REQUEST.getId()));

        testIds(podAgentSpec.getMutableWorkloadsList(), ImmutableSet.of(workloadId), TMutableWorkload::getWorkloadRef);
        assertThat(podAgentSpec.getWorkloads(0).getBoxRef(), equalTo(DefaultsPatcherV1Base.ROOT_BOX_ID));
    }

    // V2 tests
    protected void hostNameKindScenario(
            Optional<Enums.EPodHostNameKind> hostNameKindToSet,
            Enums.EPodHostNameKind expectedHostNameKind
    ) {
        TPodTemplateSpec.Builder podTemplateSpecBuilder = createDefaultPodTemplateSpecBuilder();
        hostNameKindToSet.ifPresent(
                podTemplateSpecBuilder.getSpecBuilder()::setHostNameKind
        );

        var patchResult = patch(
                DEFAULT_PATCHER_CONTEXT,
                podTemplateSpecBuilder,
                DEFAULT_UNIT_CONTEXT
        );

        assertThat(patchResult.getPodSpec().getHostNameKind(), equalTo(expectedHostNameKind));
    }

    @ParameterizedTest
    @EnumSource(
            value = Enums.EPodHostNameKind.class,
            names = {"UNRECOGNIZED"},
            mode = EnumSource.Mode.EXCLUDE
    )
    void setUserHostNameKindTest(Enums.EPodHostNameKind initialHostNameKind) {
        hostNameKindScenario(Optional.of(initialHostNameKind), initialHostNameKind);
    }

    @Test
    void setDefaultHostNameKindTest() {
        hostNameKindScenario(Optional.empty(), getDefaultHostNameKind());
    }

    protected Enums.EPodHostNameKind getDefaultHostNameKind() {
        return Enums.EPodHostNameKind.PHNK_PERSISTENT;
    }

    @Test
    void emptyMemoryLimitBeforePatchingScenario() {
        TPodTemplateSpec.Builder podTemplateSpecBuilder = createDefaultPodTemplateSpecBuilder();
        podTemplateSpecBuilder.getSpecBuilder().getResourceRequestsBuilder().clearMemoryLimit();

        Executable patch = () -> patch(
                DEFAULT_PATCHER_CONTEXT,
                podTemplateSpecBuilder,
                DEFAULT_UNIT_CONTEXT
        );

        Assertions.assertDoesNotThrow(patch);
    }

    protected enum AssertionType {
        THROWS(action -> Assertions.assertThrows(RuntimeException.class, action)),
        DOES_NOT_THROW(Assertions::assertDoesNotThrow);

        private final Consumer<Executable> assertion;

        AssertionType(Consumer<Executable> assertion) {
            this.assertion = assertion;
        }

        public Consumer<Executable> getAssertion() {
            return assertion;
        }
    }

    protected static Stream<Arguments> extraRoutesTestParametersGenerator(boolean expectedExtraRoutesForNat64) {
        return Stream.of(
                Arguments.of(EResolvConf.EResolvConf_DEFAULT, false),
                Arguments.of(EResolvConf.EResolvConf_NAT64, expectedExtraRoutesForNat64),
                Arguments.of(EResolvConf.EResolvConf_NAT64_LOCAL, expectedExtraRoutesForNat64)
        );
    }

    protected static Stream<Arguments> extraRoutesTestParametersFromV3ToLast() {
        return extraRoutesTestParametersGenerator(true);
    }

    // V3 tests
    protected void extraRoutesScenario(EResolvConf conf, boolean expectedExtraRoutes) {
        TBox box = TBox.newBuilder()
                .setResolvConf(conf)
                .build();

        TPodTemplateSpec.Builder podTemplateSpecBuilder = createDefaultPodTemplateSpecBuilder();
        DataModel.TPodSpec.Builder specBuilder = podTemplateSpecBuilder.getSpecBuilder();
        specBuilder.getPodAgentPayloadBuilder().getSpecBuilder().addBoxes(box);

        var patchResult = patch(
                DEFAULT_PATCHER_CONTEXT,
                podTemplateSpecBuilder,
                DEFAULT_UNIT_CONTEXT
        );

        assertThat(patchResult.getPodSpec().getNetworkSettings().getExtraRoutes(), equalTo(expectedExtraRoutes));
    }

    // helpful create-methods
    protected TPodTemplateSpec.Builder createDefaultPodTemplateSpecBuilder() {
        return createDefaultPodTemplateSpecBuilderFromV2ToLast();
    }

    protected static TPodTemplateSpec.Builder createDefaultPodTemplateSpecBuilderV1() {
        DataModel.TPodSpec.Builder podSpecBuilder = DataModel.TPodSpec.newBuilder()
                .addDiskVolumeRequests(USED_BY_INFRA_VOLUME_REQUEST);

        return TPodTemplateSpec.newBuilder().setSpec(podSpecBuilder);
    }

    private static TPodTemplateSpec.Builder createDefaultPodTemplateSpecBuilderFromV2ToLast() {
        TPodTemplateSpec.Builder podTemplateSpecBuilder = createDefaultPodTemplateSpecBuilderV1();
        podTemplateSpecBuilder.getSpecBuilder().getResourceRequestsBuilder().setMemoryLimit(PodSpecUtils.GIGABYTE);

        return podTemplateSpecBuilder;
    }


    protected static void checkDiskRequestExists(DataModel.TPodSpec spec, String requestId,
                                                 Optional<TAttribute> requestLabel, String diskStorageClass) {
        Optional<DataModel.TPodSpec.TDiskVolumeRequest> request = spec.getDiskVolumeRequestsList().stream()
                .filter(r -> r.getId().equals(requestId))
                .findFirst();

        assertThat(request, optionalWithValue());
        assertThat(request.orElseThrow().getStorageClass(), equalTo(diskStorageClass));

        requestLabel.ifPresent(l -> assertThat(l, in(request.get().getLabels().getAttributesList())));
    }

    public static TPodAgentSpec.Builder createPodAgentSpecBuilder() {
        return TPodAgentSpec.newBuilder()
                .addBoxes(TBox.newBuilder().setId(DEFAULT_BOX_ID))
                .addVolumes(TVolume.newBuilder().setId("volume_id"))
                .setResources(TResourceGang.newBuilder()
                        .addLayers(TLayer.newBuilder().setId("layer_id"))
                        .addStaticResources(TResource.newBuilder().setId("static_resource_id"))
                );
    }

    protected static DataModel.TPodSpec.TDiskVolumeRequest.Builder createDiskVolumeRequestBuilder(
            String allocationId,
            Optional<TAttribute> label
    ) {
        DataModel.TPodSpec.TDiskVolumeRequest.Builder request = DataModel.TPodSpec.TDiskVolumeRequest.newBuilder()
                .setId(allocationId)
                .setStorageClass("hdd");
        label.ifPresent(l -> request.setLabels(TAttributeDictionary.newBuilder()
                .addAttributes(USED_BY_INFRA_LABEL)
                .build()));
        return request;
    }

    @Test
    void patchAllowedSshKeySetTest() {
        patchAllowedSshKeySetScenario(null, null, false, Enums.EPodSshKeySet.PSKS_UNKNOWN);
        patchAllowedSshKeySetScenario(null, Enums.EPodSshKeySet.PSKS_ALL, true, Enums.EPodSshKeySet.PSKS_ALL);
        patchAllowedSshKeySetScenario("secure", null, true, Enums.EPodSshKeySet.PSKS_SECURE);
        patchAllowedSshKeySetScenario("secure", Enums.EPodSshKeySet.PSKS_ALL, true, Enums.EPodSshKeySet.PSKS_ALL);
    }

    private void patchAllowedSshKeySetScenario(String labelValue,
                                   Enums.EPodSshKeySet specValueBeforePatching,
                                   boolean expectedPresenseOfAllowedSshKeySet,
                                               Enums.EPodSshKeySet expectedValueAfterPatch) {
        TPodTemplateSpec.Builder podTemplateSpecBuilder = createDefaultPodTemplateSpecBuilder();

        DeployUnitContext deployUnitContext = labelValue == null ? DEFAULT_UNIT_CONTEXT :
                DEFAULT_UNIT_CONTEXT.withLabels(
                        ImmutableMap.of(DefaultsPatcherV1Base.ALLOWED_SSH_KEY_SET_LABEL_KEY, YTree.stringNode(labelValue)));
        if (specValueBeforePatching != null) {
            podTemplateSpecBuilder.getSpecBuilder().setAllowedSshKeySet(specValueBeforePatching);
        }

        var patchResult = patch(
                DEFAULT_PATCHER_CONTEXT,
                podTemplateSpecBuilder,
                deployUnitContext
        );

        DataModel.TPodSpec podSpec = patchResult.getPodSpec();

        assertThat(podSpec.hasAllowedSshKeySet(), equalTo(expectedPresenseOfAllowedSshKeySet));
        assertThat(podSpec.getAllowedSshKeySet(), equalTo(expectedValueAfterPatch));
    }
}

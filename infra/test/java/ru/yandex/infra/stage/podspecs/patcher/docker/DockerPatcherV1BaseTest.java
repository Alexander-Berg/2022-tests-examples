package ru.yandex.infra.stage.podspecs.patcher.docker;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.deployunit.DeployUnitContext;
import ru.yandex.infra.stage.dto.Checksum;
import ru.yandex.infra.stage.dto.DockerImageContents;
import ru.yandex.infra.stage.dto.DockerImageDescription;
import ru.yandex.infra.stage.dto.DownloadableResource;
import ru.yandex.infra.stage.podspecs.PodSpecUtils;
import ru.yandex.infra.stage.podspecs.patcher.PatcherTestBase;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.pods.TBox;
import ru.yandex.yp.client.pods.TEnvVar;
import ru.yandex.yp.client.pods.TLayer;
import ru.yandex.yp.client.pods.TPodAgentSpec;
import ru.yandex.yp.client.pods.TRootfsVolume;
import ru.yandex.yp.client.pods.TUtilityContainer;
import ru.yandex.yp.client.pods.TWorkload;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.not;
import static ru.yandex.infra.stage.TestData.DEFAULT_IMAGE_HASH;
import static ru.yandex.infra.stage.TestData.DEFAULT_UNIT_CONTEXT;
import static ru.yandex.infra.stage.TestData.DOCKER_IMAGE_DESCRIPTION;
import static ru.yandex.infra.stage.podspecs.patcher.docker.DockerPatcherV1Base.DOCKER_LAYER_ID_PREFIX;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;


abstract class DockerPatcherV1BaseTest extends PatcherTestBase<DockerPatcherV1Context> {
    protected static final String DOCKER_BOX_ID_1 = "DOCKER_BOX_ID_1";
    protected static final String DOCKER_BOX_ID_2 = "DOCKER_BOX_ID_2";
    protected static final String ALLOCATION_ID_1 = "ALLOCATION_ID_1";
    protected static final String ALLOCATION_ID_2 = "ALLOCATION_ID_2";
    protected static final String NOT_DOCKER_BOX_ID = "NOT_DOCKER_BOX_ID";
    protected static final String NOT_DOCKER_LAYER_ID = "NOT_DOCKER_LAYER_ID";
    protected static final String DOCKER_PATCH_WORKLOAD_ID = "DOCKER_PATCH_WORKLOAD_ID";
    private static final String DOCKER_CUSTOM_WORKLOAD_ID = "DOCKER_CUSTOM_WORKLOAD_ID";
    protected static final String NOT_DOCKER_WORKLOAD_ID = "NOT_DOCKER_WORKLOAD_ID";
    protected static final String DOCKER_USER = "userName";
    protected static final String DOCKER_GROUP = "groupName";
    protected static final String DOCKER_START = "'/bin/sh' '-c' 'java -jar run.jar'";
    protected static final List<String> DOCKER_START_LIST = ImmutableList.of("/bin/sh", "-c", "java -jar run.jar");
    protected static final String DOCKER_WORKING_DIR = "/home/user";
    protected static final Map<String, String> DOCKER_ENVIRONMENT = ImmutableMap.of("PATH", "/dockerDir");
    protected static final Checksum DOCKER_LAYER1_CHECKSUM = new Checksum(
            "e0e2cf1beb962090b918f511495ad80a43d3701a32f7d76428a749dc750f9216",
            Checksum.Type.SHA256);
    protected static final Checksum DOCKER_LAYER2_CHECKSUM = new Checksum(
            "5e36fb14f77cb2db7fd6801a8df873dc8e96228ec779b3c2b8c57a1607986513",
            Checksum.Type.SHA256);
    protected static final DownloadableResource DOCKER_LAYER1 = new DownloadableResource("http://host1.ru", DOCKER_LAYER1_CHECKSUM);
    protected static final DownloadableResource DOCKER_LAYER2 = new DownloadableResource("http://host2.ru", DOCKER_LAYER2_CHECKSUM);
    protected static final Map<String, DockerImageDescription> IMAGES_FOR_BOXES = ImmutableMap.of(
            DOCKER_BOX_ID_1, DOCKER_IMAGE_DESCRIPTION,
            DOCKER_BOX_ID_2, DOCKER_IMAGE_DESCRIPTION
    );
    protected static final DeployUnitContext DOCKER_UNIT_CONTEXT = DEFAULT_UNIT_CONTEXT.withDockerImages(
            ImmutableMap.of(
                    DOCKER_IMAGE_DESCRIPTION,
                    new DockerImageContents(
                            DOCKER_IMAGE_DESCRIPTION,
                            ImmutableList.of(DOCKER_LAYER1, DOCKER_LAYER2),
                            DOCKER_START_LIST,
                            Collections.emptyList(),
                            Optional.of(DOCKER_USER),
                            Optional.of(DOCKER_GROUP),
                            Optional.of(DOCKER_WORKING_DIR),
                            DOCKER_ENVIRONMENT,
                            Optional.of(DEFAULT_IMAGE_HASH)
                    )
            ),
            IMAGES_FOR_BOXES
    );

    protected static final Checksum DOCKER_LAYER3_CHECKSUM = new Checksum("3333333333333333333333333333333333333333333333333333333333333333", Checksum.Type.SHA256);
    protected static final DownloadableResource DOCKER_LAYER3 = new DownloadableResource("http://host3.ru", DOCKER_LAYER3_CHECKSUM);
    protected static final DockerImageDescription DOCKER_IMAGE_DESCRIPTION_ANOTHER_TAG = new DockerImageDescription(
            TestData.DEFAULT_REGISTRY_HOST,
            TestData.DEFAULT_IMAGE_NAME,
            TestData.DEFAULT_IMAGE_TAG + "v2");

    protected static final DockerPatcherV1Context DEFAULT_PATCHER_CONTEXT = new DockerPatcherV1Context();

    @Test
    void dockerLayerIdGenerationTest() {
        int layerNum = 0;
        String allocationId = ALLOCATION_ID_1;

        String expectedLayerId = String.format("%s-%s-%s-%d", DOCKER_LAYER_ID_PREFIX, allocationId,
                DOCKER_IMAGE_DESCRIPTION.getName(), layerNum);
        assertThatEquals(DockerPatcherV1Base.toLayerId(DOCKER_IMAGE_DESCRIPTION, allocationId, layerNum),
                expectedLayerId);
    }

    protected static <K, V> Map<K, V> collectLayersAsMap(TPodAgentSpec podAgentSpec,
                                                       Function<TLayer, ? extends K> keyMapper,
                                                       Function<TLayer, ? extends V> valueMapper) {
        return podAgentSpec.getResources().getLayersList().stream()
                .collect(Collectors.toMap(keyMapper, valueMapper));
    }

    @Test
    void addDockerImageContentsResourceTest() {
        TPodTemplateSpec.Builder podTemplateSpecBuilder = addDockerBoxes(TPodTemplateSpec.newBuilder());

        var podAgentSpec = patch(DEFAULT_PATCHER_CONTEXT, podTemplateSpecBuilder, DOCKER_UNIT_CONTEXT).getPodAgentSpec();

        Map<String, String> layerUrlToId = collectLayersAsMap(podAgentSpec, TLayer::getUrl, TLayer::getId);
        assertThat(DOCKER_LAYER1.getUrl(), in(layerUrlToId.keySet()));
        assertThat(DOCKER_LAYER2.getUrl(), in(layerUrlToId.keySet()));
        assertThat(layerUrlToId.get(DOCKER_LAYER1.getUrl()), not(equalTo(layerUrlToId.get(DOCKER_LAYER2.getUrl()))));
    }

    @Test
    void addDockerImageContentsBoxTest() {
        TPodTemplateSpec.Builder podTemplateSpecBuilder =
                addDockerWorkloads(addDockerBoxes(TPodTemplateSpec.newBuilder()));

        var podAgentSpec = patch(DEFAULT_PATCHER_CONTEXT, podTemplateSpecBuilder, DOCKER_UNIT_CONTEXT).getPodAgentSpec();

        Map<String, String> layerUrlToId = collectLayersAsMap(podAgentSpec, TLayer::getUrl, TLayer::getId);
        String id1 = layerUrlToId.get(DOCKER_LAYER1.getUrl());
        String id2 = layerUrlToId.get(DOCKER_LAYER2.getUrl());
        TBox dockerBox = getBox(podAgentSpec, DOCKER_BOX_ID_1);
        List<String> expectedLayerRefsList = ImmutableList.of(NOT_DOCKER_LAYER_ID, id2, id1);
        assertThatEquals(dockerBox.getRootfs().getLayerRefsList(), expectedLayerRefsList);
    }

    protected abstract String generateLayerId(DockerImageDescription description,
                                              String allocationId,
                                              int layerNum,
                                              DownloadableResource resource);

    @Test
    void addDockerImageContentsBoxesWithAllocationsTest() {
        TPodTemplateSpec.Builder podTemplateSpecBuilder =
                addDockerWorkloads(addDockerBoxesWithAllocations(TPodTemplateSpec.newBuilder()));

        var podAgentSpec = patch(DEFAULT_PATCHER_CONTEXT, podTemplateSpecBuilder, DOCKER_UNIT_CONTEXT).getPodAgentSpec();

        Map<String, String> layerIdToUrl = collectLayersAsMap(podAgentSpec, TLayer::getId, TLayer::getUrl);

        String id1 = generateLayerId(DOCKER_IMAGE_DESCRIPTION, ALLOCATION_ID_1, 0, DOCKER_LAYER1);
        String id2 = generateLayerId(DOCKER_IMAGE_DESCRIPTION, ALLOCATION_ID_1, 1, DOCKER_LAYER2);

        String id3 = generateLayerId(DOCKER_IMAGE_DESCRIPTION, ALLOCATION_ID_2, 0, DOCKER_LAYER1);
        String id4 = generateLayerId(DOCKER_IMAGE_DESCRIPTION, ALLOCATION_ID_2, 1, DOCKER_LAYER2);

        Map<String, String> expectedLayerIdToUrl = ImmutableMap.of(id1, DOCKER_LAYER1.getUrl(),
                id2, DOCKER_LAYER2.getUrl(),
                id3, DOCKER_LAYER1.getUrl(),
                id4, DOCKER_LAYER2.getUrl());

        assertThatEquals(layerIdToUrl, expectedLayerIdToUrl);

        TBox dockerBox1 = getBox(podAgentSpec, DOCKER_BOX_ID_1);
        TBox dockerBox2 = getBox(podAgentSpec, DOCKER_BOX_ID_2);
        TBox notDockerBox = getBox(podAgentSpec, NOT_DOCKER_BOX_ID);
        List<String> expectedLayersBox1 = ImmutableList.of(NOT_DOCKER_LAYER_ID, id2, id1);
        List<String> expectedLayersBox2 = ImmutableList.of(NOT_DOCKER_LAYER_ID, id4, id3);
        List<String> expectedLayersNotDockerBox = ImmutableList.of(NOT_DOCKER_LAYER_ID);

        assertThatEquals(dockerBox1.getRootfs().getLayerRefsList(), expectedLayersBox1);
        assertThatEquals(dockerBox2.getRootfs().getLayerRefsList(), expectedLayersBox2);
        assertThatEquals(notDockerBox.getRootfs().getLayerRefsList(), expectedLayersNotDockerBox);
    }

    protected void checkWorkloadVariable(TEnvVar expectedVar, List<TEnvVar> workloadVars) {
        assertThat(expectedVar, in(workloadVars));
    }

    @Test
    void addDockerImageContentsWorkloadPatchTest() {
        TPodTemplateSpec.Builder podTemplateSpecBuilder =
                addDockerWorkloads(addDockerBoxes(TPodTemplateSpec.newBuilder()));

        var podAgentSpec = patch(DEFAULT_PATCHER_CONTEXT, podTemplateSpecBuilder, DOCKER_UNIT_CONTEXT).getPodAgentSpec();

        TBox dockerBox = getBox(podAgentSpec, DOCKER_BOX_ID_1);
        TBox notDockerBox = getBox(podAgentSpec, NOT_DOCKER_BOX_ID);

        TWorkload dockerWorkload = getWorkload(podAgentSpec, DOCKER_PATCH_WORKLOAD_ID);
        var dockerWorkloadStart = dockerWorkload.getStart();
        assertThatEquals(dockerWorkloadStart.getUser(), DOCKER_USER);
        assertThatEquals(dockerWorkloadStart.getCommandLine(), DOCKER_START);
        assertThatEquals(dockerWorkloadStart.getGroup(), DOCKER_GROUP);
        assertThatEquals(dockerWorkloadStart.getCwd(), DOCKER_WORKING_DIR);
        DOCKER_ENVIRONMENT.forEach((key, value) -> {
            TEnvVar expectedVar = PodSpecUtils.literalEnvVar(key, value);
            checkWorkloadVariable(expectedVar, dockerWorkload.getEnvList());
            assertThat(expectedVar, in(dockerBox.getEnvList()));
            assertThat(expectedVar, not(in(notDockerBox.getEnvList())));
        });
    }

    @Test
    void notOverrideFilledWorkloadFieldsTest() {
        TPodTemplateSpec.Builder podTemplateSpecBuilder = addDockerBoxes(TPodTemplateSpec.newBuilder());
        TWorkload.Builder dockerCustomWorkloadBuilder = TWorkload.newBuilder();
        DOCKER_ENVIRONMENT.forEach((key, value) ->
                dockerCustomWorkloadBuilder.addEnv(PodSpecUtils.literalEnvVar(key, "NOT_DOCKER_VALUE")));
        podTemplateSpecBuilder.getSpecBuilder().getPodAgentPayloadBuilder().getSpecBuilder()
                .addWorkloads(dockerCustomWorkloadBuilder
                        .setId(DOCKER_CUSTOM_WORKLOAD_ID)
                        .setBoxRef(DOCKER_BOX_ID_1)
                        .setStart(TUtilityContainer.newBuilder()
                                .setCommandLine("NOT_DOCKER_COMMAND")
                                .setGroup("NOT_DOCKER_GROUP")
                                .setUser("NOT_DOCKER_USER")
                                .setCwd("NOT_DOCKER_DIR")));

        var podAgentSpec = patch(DEFAULT_PATCHER_CONTEXT, podTemplateSpecBuilder, DEFAULT_UNIT_CONTEXT).getPodAgentSpec();

        TWorkload dockerWorkload = getWorkload(podAgentSpec, DOCKER_CUSTOM_WORKLOAD_ID);
        var dockerWorkloadStart = dockerWorkload.getStart();
        assertThat(dockerWorkloadStart.getUser(), not(equalTo(DOCKER_USER)));
        assertThat(dockerWorkloadStart.getCommandLine(), not(equalTo(DOCKER_START)));
        assertThat(dockerWorkloadStart.getGroup(), not(equalTo(DOCKER_GROUP)));
        assertThat(dockerWorkloadStart.getCwd(), not(equalTo(DOCKER_WORKING_DIR)));

        DOCKER_ENVIRONMENT.forEach((key, value) ->
                assertThat(PodSpecUtils.literalEnvVar(key, value), not(in(dockerWorkload.getEnvList()))));
    }

    @Test
    void notOverrideFilledBoxFieldsTest() {
        TPodTemplateSpec.Builder podTemplateSpecBuilder = TPodTemplateSpec.newBuilder();
        TBox.Builder boxBuilder = TBox.newBuilder()
                .setId(DOCKER_BOX_ID_1)
                .setRootfs(TRootfsVolume.newBuilder()
                        .addLayerRefs(NOT_DOCKER_LAYER_ID));
        DOCKER_ENVIRONMENT.forEach((key, value) ->
                boxBuilder.addEnv(PodSpecUtils.literalEnvVar(key, "NOT_DOCKER_VALUE")));
        podTemplateSpecBuilder.getSpecBuilder().getPodAgentPayloadBuilder().getSpecBuilder()
                .addBoxes(boxBuilder);

        var podAgentSpec = patch(DEFAULT_PATCHER_CONTEXT, podTemplateSpecBuilder, DEFAULT_UNIT_CONTEXT).getPodAgentSpec();

        TBox dockerBox = getBox(podAgentSpec, DOCKER_BOX_ID_1);
        DOCKER_ENVIRONMENT.forEach((key, value) ->
                assertThat(PodSpecUtils.literalEnvVar(key, value), not(in(dockerBox.getEnvList()))));
    }

    @Test
    void escapingTest() {
        TPodTemplateSpec.Builder podTemplateSpecBuilder =
                addDockerWorkloads(addDockerBoxes(TPodTemplateSpec.newBuilder()));

        DeployUnitContext deployUnitContext =
                DOCKER_UNIT_CONTEXT.withDockerImages(
                        ImmutableMap.of(
                                DOCKER_IMAGE_DESCRIPTION,
                                new DockerImageContents(
                                        DOCKER_IMAGE_DESCRIPTION,
                                        Collections.emptyList(),
                                        List.of("aaa ' aaa"),
                                        Collections.emptyList(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Optional.empty(),
                                        Collections.emptyMap(),
                                        Optional.of(DEFAULT_IMAGE_HASH)
                                )
                        ),
                        IMAGES_FOR_BOXES
                );

        var podAgentSpec = patch(DEFAULT_PATCHER_CONTEXT, podTemplateSpecBuilder, deployUnitContext).getPodAgentSpec();

        TWorkload dockerWorkload = getWorkload(podAgentSpec, DOCKER_PATCH_WORKLOAD_ID);
        assertThatEquals(dockerWorkload.getStart().getCommandLine(), "'aaa '\"'\"' aaa'");
    }

    protected static TPodTemplateSpec.Builder addDockerBoxes(TPodTemplateSpec.Builder templateSpec) {
        templateSpec.getSpecBuilder().getPodAgentPayloadBuilder().getSpecBuilder()
                .addBoxes(TBox.newBuilder()
                        .setId(DOCKER_BOX_ID_1)
                        .setRootfs(TRootfsVolume.newBuilder()
                                .addLayerRefs(NOT_DOCKER_LAYER_ID)
                                .build())
                        .build())
                .addBoxes(TBox.newBuilder()
                        .setId(NOT_DOCKER_BOX_ID)
                        .build());
        return templateSpec;
    }

    private static TPodTemplateSpec.Builder addDockerBoxesWithAllocations(TPodTemplateSpec.Builder podTemplateSpecBuilder) {
        podTemplateSpecBuilder.getSpecBuilder().getPodAgentPayloadBuilder().getSpecBuilder()
                .addBoxes(TBox.newBuilder()
                        .setId(DOCKER_BOX_ID_1)
                        .setRootfs(TRootfsVolume.newBuilder()
                                .addLayerRefs(NOT_DOCKER_LAYER_ID)
                                .build())
                        .setVirtualDiskIdRef(ALLOCATION_ID_1)
                        .build())
                .addBoxes(TBox.newBuilder()
                        .setId(DOCKER_BOX_ID_2)
                        .setRootfs(TRootfsVolume.newBuilder()
                                .addLayerRefs(NOT_DOCKER_LAYER_ID)
                                .build())
                        .setVirtualDiskIdRef(ALLOCATION_ID_2)
                        .build())
                .addBoxes(TBox.newBuilder()
                        .setId(NOT_DOCKER_BOX_ID)
                        .setRootfs(TRootfsVolume.newBuilder()
                                .addLayerRefs(NOT_DOCKER_LAYER_ID)
                                .build())
                        .setVirtualDiskIdRef(ALLOCATION_ID_2)
                        .build());
        return podTemplateSpecBuilder;
    }

    protected TPodTemplateSpec.Builder addDockerBoxesWithTheSameImageButDifferentTags() {
        TPodTemplateSpec.Builder podTemplateSpecBuilder = TPodTemplateSpec.newBuilder();
        podTemplateSpecBuilder.getSpecBuilder().getPodAgentPayloadBuilder().getSpecBuilder()
                .addBoxes(TBox.newBuilder()
                        .setId(DOCKER_BOX_ID_1)
                        .setVirtualDiskIdRef(ALLOCATION_ID_1)
                        .setRootfs(TRootfsVolume.newBuilder()
                                .addLayerRefs(NOT_DOCKER_LAYER_ID)
                                .build())
                        .build())
                .addBoxes(TBox.newBuilder()
                        .setId(DOCKER_BOX_ID_2)
                        .setVirtualDiskIdRef(ALLOCATION_ID_1)
                        .setRootfs(TRootfsVolume.newBuilder()
                                .addLayerRefs(NOT_DOCKER_LAYER_ID)
                                .build())
                        .build());

        return podTemplateSpecBuilder;
    }

    protected static TPodTemplateSpec.Builder addDockerWorkloads(TPodTemplateSpec.Builder podTemplateSpecBuilder) {
        podTemplateSpecBuilder.getSpecBuilder().getPodAgentPayloadBuilder().getSpecBuilder()
                .addWorkloads(TWorkload.newBuilder()
                        .setBoxRef(DOCKER_BOX_ID_1)
                        .setId(DOCKER_PATCH_WORKLOAD_ID)
                        .build())
                .addWorkloads(TWorkload.newBuilder()
                        .setBoxRef(NOT_DOCKER_BOX_ID)
                        .setId(NOT_DOCKER_WORKLOAD_ID)
                        .build());
        return podTemplateSpecBuilder;
    }

    protected static TWorkload getWorkload(TPodAgentSpec podAgentSpec, String id) {
        return podAgentSpec.getWorkloadsList().stream()
                .filter(workload -> workload.getId().equals(id)).findAny().orElseThrow();
    }

    protected static TBox getBox(TPodAgentSpec podAgentSpec, String id) {
        return podAgentSpec.getBoxesList().stream()
                .filter(box -> box.getId().equals(id)).findAny().orElseThrow();
    }

    protected DockerImageContents getContentsByLayers(List<DownloadableResource> layers) {
        return new DockerImageContents(
                DOCKER_IMAGE_DESCRIPTION,
                layers,
                DOCKER_START_LIST,
                Collections.emptyList(),
                Optional.of(DOCKER_USER),
                Optional.of(DOCKER_GROUP),
                Optional.of(DOCKER_WORKING_DIR),
                DOCKER_ENVIRONMENT,
                Optional.of(TestData.DEFAULT_IMAGE_HASH));
    }
}

package ru.yandex.infra.stage.podspecs.patcher.docker;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.deployunit.DeployUnitContext;
import ru.yandex.infra.stage.dto.DockerImageDescription;
import ru.yandex.infra.stage.dto.DownloadableResource;
import ru.yandex.infra.stage.podspecs.PodSpecUtils;
import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.pods.TBox;
import ru.yandex.yp.client.pods.TEnvVar;
import ru.yandex.yp.client.pods.TLayer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.not;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

public class DockerPatcherV2Test extends DockerPatcherV1BaseTest {
    @Override
    protected Function<DockerPatcherV1Context, ? extends SpecPatcher<TPodTemplateSpec.Builder>> getPatcherConstructor() {
        return DockerPatcherV2::new;
    }

    @Override
    protected String generateLayerId(DockerImageDescription description,
                                     String allocationId,
                                     int layerNum,
                                     DownloadableResource resource){
        return DockerPatcherV2.toLayerIdV2(allocationId, resource);
    }

    @Test
    void patchBoxEnvVarsForDockerTest() {
        TPodTemplateSpec.Builder podTemplateSpecBuilder = addDockerWorkloads(addDockerBoxes(TPodTemplateSpec.newBuilder()));

        var podAgentSpec = patch(DEFAULT_PATCHER_CONTEXT, podTemplateSpecBuilder, DOCKER_UNIT_CONTEXT).getPodAgentSpec();

        TBox dockerBox = getBox(podAgentSpec, DOCKER_BOX_ID_1);
        TBox notDockerBox = getBox(podAgentSpec, NOT_DOCKER_BOX_ID);

        String expectedImageName = String.format("%s/%s:%s", TestData.DEFAULT_REGISTRY_HOST, TestData.DEFAULT_IMAGE_NAME, TestData.DEFAULT_IMAGE_TAG);

        Map<String, String> expectedVars = ImmutableMap.of(DockerPatcherV2.DEPLOY_DOCKER_IMAGE_ENV_NAME, expectedImageName,
                                                           DockerPatcherV2.DEPLOY_DOCKER_HASH_ENV_NAME, TestData.DEFAULT_IMAGE_HASH);

        expectedVars.forEach((key,value) -> {
            TEnvVar envVar = PodSpecUtils.literalEnvVar(key, value);
            assertThat(envVar, in(dockerBox.getEnvList()));
            assertThat(envVar, not(in(notDockerBox.getEnvList())));
        });
    }

    @Test
    void theSameImageDifferentTagsTest() {
        var podTemplateSpecBuilder = addDockerBoxesWithTheSameImageButDifferentTags();

        DeployUnitContext context = TestData.DEFAULT_UNIT_CONTEXT.withDockerImages(
                ImmutableMap.of(
                        TestData.DOCKER_IMAGE_DESCRIPTION,
                        getContentsByLayers(ImmutableList.of(DOCKER_LAYER1, DOCKER_LAYER2)),
                        DOCKER_IMAGE_DESCRIPTION_ANOTHER_TAG,
                        getContentsByLayers(ImmutableList.of(DOCKER_LAYER1, DOCKER_LAYER3))
                ),
                ImmutableMap.of(
                        DOCKER_BOX_ID_1, TestData.DOCKER_IMAGE_DESCRIPTION,
                        DOCKER_BOX_ID_2, DOCKER_IMAGE_DESCRIPTION_ANOTHER_TAG
                )
        );

        var podAgentSpec = patch(DEFAULT_PATCHER_CONTEXT, podTemplateSpecBuilder, context).getPodAgentSpec();


        Map<String, String> layerIdToUrl = collectLayersAsMap(podAgentSpec, TLayer::getId, TLayer::getUrl);

        String id1 = DockerPatcherV2.toLayerIdV2(ALLOCATION_ID_1, DOCKER_LAYER1);
        String id2 = DockerPatcherV2.toLayerIdV2(ALLOCATION_ID_1, DOCKER_LAYER2);
        String id3 = DockerPatcherV2.toLayerIdV2(ALLOCATION_ID_1, DOCKER_LAYER3);

        Map<String, String> expectedLayerIdToUrl = ImmutableMap.of(id1, DOCKER_LAYER1.getUrl(),
                                                                   id2, DOCKER_LAYER2.getUrl(),
                                                                   id3, DOCKER_LAYER3.getUrl());

        //before DEPLOY-3713 layerIdToUrl contains only 2 layers...
        assertThatEquals(layerIdToUrl, expectedLayerIdToUrl);

        TBox dockerBox1 = getBox(podAgentSpec, DOCKER_BOX_ID_1);
        List<String> expectedLayersBox1 = ImmutableList.of(NOT_DOCKER_LAYER_ID, id2, id1);
        assertThatEquals(dockerBox1.getRootfs().getLayerRefsList(), expectedLayersBox1);

        TBox dockerBox2 = getBox(podAgentSpec, DOCKER_BOX_ID_2);
        List<String> expectedLayersBox2 = ImmutableList.of(NOT_DOCKER_LAYER_ID, id3, id1);
        assertThatEquals(dockerBox2.getRootfs().getLayerRefsList(), expectedLayersBox2);
    }

    @Test
    void removeDuplicatedTagsTest() {
        var podTemplateSpecBuilder = addDockerBoxesWithTheSameImageButDifferentTags();

        DeployUnitContext context = TestData.DEFAULT_UNIT_CONTEXT.withDockerImages(
                ImmutableMap.of(
                        TestData.DOCKER_IMAGE_DESCRIPTION,
                        getContentsByLayers(ImmutableList.of(
                                DOCKER_LAYER1, DOCKER_LAYER2, DOCKER_LAYER1, DOCKER_LAYER3
                        ))
                ),
                ImmutableMap.of(DOCKER_BOX_ID_1, TestData.DOCKER_IMAGE_DESCRIPTION)
        );

        var podAgentSpec = patch(DEFAULT_PATCHER_CONTEXT, podTemplateSpecBuilder, context).getPodAgentSpec();


        List<TLayer> layers = podAgentSpec.getResources().getLayersList();

        String id1 = DockerPatcherV2.toLayerIdV2(ALLOCATION_ID_1, DOCKER_LAYER1);
        String id2 = DockerPatcherV2.toLayerIdV2(ALLOCATION_ID_1, DOCKER_LAYER2);
        String id3 = DockerPatcherV2.toLayerIdV2(ALLOCATION_ID_1, DOCKER_LAYER3);

        assertThatEquals(layers.size(), 3);
        assertThatEquals(layers.get(0).getId(), id1);
        assertThatEquals(layers.get(1).getId(), id2);
        assertThatEquals(layers.get(2).getId(), id3);

        TBox dockerBox1 = getBox(podAgentSpec, DOCKER_BOX_ID_1);
        List<String> expectedLayersBox1 = ImmutableList.of(NOT_DOCKER_LAYER_ID, id3, id1, id2);
        assertThatEquals(dockerBox1.getRootfs().getLayerRefsList(), expectedLayersBox1);
    }

}

package ru.yandex.infra.stage.podspecs.patcher.docker;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.deployunit.DeployUnitContext;
import ru.yandex.infra.stage.dto.DockerImageDescription;
import ru.yandex.infra.stage.dto.DownloadableResource;
import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.pods.TBox;
import ru.yandex.yp.client.pods.TEnvVar;
import ru.yandex.yp.client.pods.TLayer;
import ru.yandex.yp.client.pods.TPodAgentSpec;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.not;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

public class DockerPatcherV1Test extends DockerPatcherV1BaseTest {

    @Override
    protected Function<DockerPatcherV1Context, ? extends SpecPatcher<TPodTemplateSpec.Builder>> getPatcherConstructor() {
        return DockerPatcherV1::new;
    }

    @Override
    protected String generateLayerId(DockerImageDescription description,
                                     String allocationId,
                                     int layerNum,
                                     DownloadableResource resource){
        return DockerPatcherV1Base.toLayerId(description, allocationId, layerNum);
    }

    @Test
    void noChangesFromV2_addBoxEnvVarsForDockerTest() {
        TPodTemplateSpec.Builder podTemplateSpecBuilder = addDockerWorkloads(addDockerBoxes(TPodTemplateSpec.newBuilder()));

        var podAgentSpec = patch(DEFAULT_PATCHER_CONTEXT, podTemplateSpecBuilder, DOCKER_UNIT_CONTEXT).getPodAgentSpec();

        TBox dockerBox = getBox(podAgentSpec, DOCKER_BOX_ID_1);

        ImmutableList<String> varsFromV2 = ImmutableList.of(DockerPatcherV2.DEPLOY_DOCKER_IMAGE_ENV_NAME, DockerPatcherV2.DEPLOY_DOCKER_HASH_ENV_NAME);
        Set<String> boxEnvVarNames = dockerBox.getEnvList()
                                              .stream()
                                              .map(TEnvVar::getName)
                                              .collect(Collectors.toSet());

        varsFromV2.forEach(key -> assertThat(key, not(in(boxEnvVarNames))));
    }

    @Test
    void noChangesFromV2_theSameImageDifferentTagsTest() {
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

        TPodAgentSpec podAgentSpec = patch(DEFAULT_PATCHER_CONTEXT, podTemplateSpecBuilder, context).getPodAgentSpec();

        //Here we can see randomly DOCKER_LAYER2 or DOCKER_LAYER3,
        //  it depends on map iteration order (not guaranteed by java runtime)
        var secondLayerUrl = podAgentSpec.getResources().getLayersList().get(1).getUrl();
        var secondLayer = DOCKER_LAYER2.getUrl().equals(secondLayerUrl) ? DOCKER_LAYER2 : DOCKER_LAYER3;

        Map<String, String> layerIdToUrl = collectLayersAsMap(podAgentSpec, TLayer::getId, TLayer::getUrl);

        String id1 = generateLayerId(TestData.DOCKER_IMAGE_DESCRIPTION, ALLOCATION_ID_1, 0, DOCKER_LAYER1);
        String id2 = generateLayerId(TestData.DOCKER_IMAGE_DESCRIPTION, ALLOCATION_ID_1, 1, secondLayer);

        Map<String, String> expectedLayerIdToUrl = ImmutableMap.of(id1, DOCKER_LAYER1.getUrl(),
                                                                   id2, secondLayer.getUrl());

        //DOCKER_LAYER3 is missed, bug in DockerPatcherV1
        assertThatEquals(layerIdToUrl, expectedLayerIdToUrl);

        TBox dockerBox1 = getBox(podAgentSpec, DOCKER_BOX_ID_1);
        List<String> expectedLayersBox1 = ImmutableList.of(NOT_DOCKER_LAYER_ID, id2, id1);
        assertThatEquals(dockerBox1.getRootfs().getLayerRefsList(), expectedLayersBox1);

        TBox dockerBox2 = getBox(podAgentSpec, DOCKER_BOX_ID_2);
        //DOCKER_LAYER3 is missed, bug in DockerPatcherV1
        List<String> expectedLayersBox2 = ImmutableList.of(NOT_DOCKER_LAYER_ID, id2, id1);
        assertThatEquals(dockerBox2.getRootfs().getLayerRefsList(), expectedLayersBox2);
    }

}

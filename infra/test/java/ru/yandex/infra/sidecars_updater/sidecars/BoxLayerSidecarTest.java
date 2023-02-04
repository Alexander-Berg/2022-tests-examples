package ru.yandex.infra.sidecars_updater.sidecars;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.infra.sidecars_updater.sandbox.SandboxInfoGetter;
import ru.yandex.infra.sidecars_updater.sandbox.SandboxResourceInfo;
import ru.yandex.yp.client.api.TDeployUnitSpec;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.api.TStageSpec;
import ru.yandex.yp.client.pods.TLayer;
import ru.yandex.yp.client.pods.TResourceGang;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.infra.sidecars_updater.TestData.DEFAULT_LAYER_ATTRIBUTES;
import static ru.yandex.infra.sidecars_updater.TestData.DEFAULT_LAYER_INFO;
import static ru.yandex.infra.sidecars_updater.util.Utils.getOtherValue;

public class BoxLayerSidecarTest {
    public static final List<Sidecar.Type> LAYER_TYPES = List.of(
            Sidecar.Type.PORTO_LAYER_SEARCH_UBUNTU_XENIAL_APP,
            Sidecar.Type.PORTO_LAYER_SEARCH_UBUNTU_BIONIC_APP,
            Sidecar.Type.PORTO_LAYER_SEARCH_UBUNTU_FOCAL_APP
    );
    private static final String DEFAULT_DEPLOY_UNIT_NAME = "deployUnit1";
    private static final String DEFAULT_LABEL = "SidecarLabel";
    private static final String DEFAULT_LAYER_ID = "layer-1";

    private static Stream<Arguments> isUsedByTestParameters() {
        Stream.Builder<Arguments> streamBuilder = Stream.builder();
        for (var type : LAYER_TYPES) {
            Sidecar.Type otherType = getOtherValue(LAYER_TYPES, type);
            streamBuilder.add(
                    Arguments.of(type, Optional.empty(), false)
            );
            streamBuilder.add(
                    Arguments.of(type, Optional.of(DEFAULT_LAYER_INFO.withType(otherType.toString())), false)
            );
            streamBuilder.add(
                    Arguments.of(type, Optional.of(DEFAULT_LAYER_INFO.withType(type.toString())), true)
            );
        }
        return streamBuilder.build();
    }

    @ParameterizedTest
    @MethodSource("isUsedByTestParameters")
    void isUsedByTest(Sidecar.Type type, Optional<SandboxResourceInfo> layerInfo, boolean expectedIsUsedBy) {
        SandboxInfoGetter sandboxInfoGetter = mock(SandboxInfoGetter.class);
        String layerUrl = "layer_url";
        when(sandboxInfoGetter.getSandboxResourceInfoByUrl(eq(layerUrl))).thenReturn(layerInfo);

        Map<Sidecar.Type, Map<String, String>> sidecarsAttributes = Map.of(type, DEFAULT_LAYER_ATTRIBUTES);
        Sidecar boxLayerSidecar = new BoxLayerSidecar(sidecarsAttributes, DEFAULT_LABEL, type, sandboxInfoGetter);

        TDeployUnitSpec.Builder deployUnitSpecBuilder = TDeployUnitSpec.newBuilder();
        TLayer.Builder layerBuilder = TLayer.newBuilder().setId(DEFAULT_LAYER_ID).setUrl(layerUrl);
        getResourcesBuilder(deployUnitSpecBuilder).addLayers(layerBuilder);
        TStageSpec.Builder stageSpecBuilder = TStageSpec.newBuilder().putDeployUnits(
                DEFAULT_DEPLOY_UNIT_NAME, deployUnitSpecBuilder.build()
        );

        boolean actualIsUsedBy = boxLayerSidecar.isUsedBy(stageSpecBuilder.build(), DEFAULT_DEPLOY_UNIT_NAME);
        assertThat(actualIsUsedBy, equalTo(expectedIsUsedBy));
    }

    private static TResourceGang.Builder getResourcesBuilder(TDeployUnitSpec.Builder deployUnitSpecBuilder) {
        return getPodTemplateSpecBuilder(deployUnitSpecBuilder)
                .getSpecBuilder()
                .getPodAgentPayloadBuilder()
                .getSpecBuilder()
                .getResourcesBuilder();
    }

    private static TPodTemplateSpec.Builder getPodTemplateSpecBuilder(TDeployUnitSpec.Builder deployUnitSpecBuilder) {
        return deployUnitSpecBuilder.hasMultiClusterReplicaSet() ?
                deployUnitSpecBuilder.getMultiClusterReplicaSetBuilder().getReplicaSetBuilder().getPodTemplateSpecBuilder() :
                deployUnitSpecBuilder.getReplicaSetBuilder().getReplicaSetTemplateBuilder().getPodTemplateSpecBuilder();
    }
}

package ru.yandex.infra.stage.podspecs.patcher.endpoint_set_liveness;

import org.junit.jupiter.api.Test;

import ru.yandex.infra.stage.deployunit.DeployUnitContext;
import ru.yandex.infra.stage.dto.DeployUnitSpec;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.yp.client.api.DataModel;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EndpointSetSpecPatcherV2Test {
    public static final double DEFAULT_LIVENESS_RATIO_V2 = 1.0;

    @Test
    public void patchDefaults() {
        DeployUnitContext deployUnitContext = mock(DeployUnitContext.class);
        DeployUnitSpec deployUnitSpec = mock(DeployUnitSpec.class);
        when(deployUnitContext.getSpec()).thenReturn(deployUnitSpec);

        DataModel.TEndpointSetSpec.Builder endpointSpecBuilder = DataModel.TEndpointSetSpec.newBuilder();
        new EndpointSetLivenessPatcherV2().patch(endpointSpecBuilder, deployUnitContext, new YTreeBuilder());

        assertThat(endpointSpecBuilder.getLivenessLimitRatio(), equalTo(DEFAULT_LIVENESS_RATIO_V2));
    }
}

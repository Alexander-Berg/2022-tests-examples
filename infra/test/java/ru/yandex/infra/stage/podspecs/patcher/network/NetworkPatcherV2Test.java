package ru.yandex.infra.stage.podspecs.patcher.network;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.stage.deployunit.DeployUnitContext;
import ru.yandex.infra.stage.dto.DeployUnitSpecDetails;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.yp.client.api.DataModel;
import ru.yandex.yp.client.api.TPodTemplateSpec;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.infra.stage.TestData.DEFAULT_UNIT_CONTEXT;
import static ru.yandex.infra.stage.podspecs.patcher.network.NetworkPatcherV2.LABEL_NAME_IP_LIMIT;

public class NetworkPatcherV2Test {

    public YTreeBuilder ytBuilder;

    @BeforeEach
    public void before() {
        ytBuilder = new YTreeBuilder();
    }

    @Test
    public void checkIpLimitsWithLabel() {
        NetworkPatcherV2 networkPatcherV2 = new NetworkPatcherV2();
        DeployUnitContext contextWithLabel = DEFAULT_UNIT_CONTEXT.withLabels(
                ImmutableMap.of(LABEL_NAME_IP_LIMIT, ytBuilder.entity().build())
        );
        TPodTemplateSpec.Builder builder = TPodTemplateSpec.newBuilder();
        networkPatcherV2.patch(builder, contextWithLabel, ytBuilder);
        assertThat(builder.getSpec().getNetworkSettings().getIpLimit(), equalTo(false));
    }

    @Test
    public void checkIpLimitsWithoutLabel() {
        NetworkPatcherV2 networkPatcherV2 = new NetworkPatcherV2();
        TPodTemplateSpec.Builder builder = TPodTemplateSpec.newBuilder();
        networkPatcherV2.patch(builder, DEFAULT_UNIT_CONTEXT, ytBuilder);
        assertThat(builder.getSpec().getNetworkSettings().getIpLimit(), equalTo(true));
    }

    @Test
    public void checkExtraRoutesWithEmptyData() {
        NetworkPatcherV2 networkPatcherV2 = new NetworkPatcherV2();
        TPodTemplateSpec.Builder builder = TPodTemplateSpec.newBuilder();
        networkPatcherV2.patch(builder, DEFAULT_UNIT_CONTEXT, ytBuilder);
        assertThat(builder.getSpec().getNetworkSettings().getExtraRoutes(), equalTo(false));
    }

    @Test
    public void checkExtraRoutesWithFillData() {
        NetworkPatcherV2 networkPatcherV2 = new NetworkPatcherV2();
        TPodTemplateSpec.Builder builder = TPodTemplateSpec.newBuilder();
        DataModel.TPodSpec.Builder tPodSpec = DataModel.TPodSpec.newBuilder();
        tPodSpec.getNetworkSettingsBuilder().setExtraRoutes(true);
        DeployUnitSpecDetails unitSpecDetails = mock(DeployUnitSpecDetails.class);
        when(unitSpecDetails.getPodSpec()).thenReturn(tPodSpec.build());
        DeployUnitContext deployUnitContext = DEFAULT_UNIT_CONTEXT.withSpecDetails(unitSpecDetails);
        networkPatcherV2.patch(builder, deployUnitContext, ytBuilder);
        assertThat(builder.getSpec().getNetworkSettings().getExtraRoutes(), equalTo(true));
    }
}

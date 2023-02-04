package ru.yandex.infra.stage.podspecs.patcher.network;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.dto.NetworkDefaults;
import ru.yandex.infra.stage.podspecs.patcher.defaults.DefaultsPatcherV1Base;
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.yp.client.api.DataModel;
import ru.yandex.yp.client.api.TPodTemplateSpec;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.infra.stage.TestData.DEFAULT_UNIT_CONTEXT;
import static ru.yandex.infra.stage.TestData.NETWORK_DEFAULTS;
import static ru.yandex.infra.stage.TestData.NETWORK_ID;
import static ru.yandex.infra.stage.podspecs.patcher.defaults.DefaultsPatcherV1BaseTest.CHANGED_NETWORK_ID;
import static ru.yandex.infra.stage.podspecs.patcher.defaults.DefaultsPatcherV1BaseTest.createPodAgentSpecBuilder;
import static ru.yandex.infra.stage.util.AssertUtils.assertCollectionMatched;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

public class NetworkPatcherV3Test {

    public YTreeBuilder ytBuilder;
    public NetworkPatcherV3 networkPatcherV3;

    @BeforeEach
    public void before() {
        ytBuilder = new YTreeBuilder();
        networkPatcherV3 = new NetworkPatcherV3();
    }

    @Test
    void patchNetworksDefaultsWithIp4AddressPool() {
        String poolId = "pool_id";

        TPodTemplateSpec.Builder builder = TPodTemplateSpec.newBuilder();
        NetworkDefaults networkDefaults = NETWORK_DEFAULTS.withIp4AddressPoolId(poolId);
        var deployUnitContext = DEFAULT_UNIT_CONTEXT.withNetworkDefaults(networkDefaults);
        networkPatcherV3.patch(builder, deployUnitContext, ytBuilder);

        DataModel.TPodSpec podSpec = builder.getSpec();

        Map<String, DataModel.TPodSpec.TIP6AddressRequest> networks =
                podSpec.getIp6AddressRequestsList().stream().collect(Collectors.toMap(DataModel.TPodSpec.TIP6AddressRequest::getVlanId, v -> v));
        assertThatEquals(networks.get(DefaultsPatcherV1Base.BACKBONE_VLAN).getIp4AddressPoolId(), poolId);
        assertThat(networks.get(DefaultsPatcherV1Base.FASTBONE_VLAN).hasIp4AddressPoolId(), equalTo(false));
    }

    @Test
    void patchNetworksDefaultsWithVirtualServiceIds() {
        List<String> virtualServiceIds = ImmutableList.of("s1", "s2");

        TPodTemplateSpec.Builder builder = TPodTemplateSpec.newBuilder();
        NetworkDefaults networkDefaults = NETWORK_DEFAULTS.toBuilder()
                .withOverrideIp6AddressRequests(true)
                .withVirtualServiceIds(virtualServiceIds)
                .build();

        var deployUnitContext = DEFAULT_UNIT_CONTEXT.withNetworkDefaults(networkDefaults);
        networkPatcherV3.patch(builder, deployUnitContext, ytBuilder);

        DataModel.TPodSpec podSpec = builder.getSpec();

        Map<String, DataModel.TPodSpec.TIP6AddressRequest> networks =
                podSpec.getIp6AddressRequestsList().stream().collect(Collectors.toMap(DataModel.TPodSpec.TIP6AddressRequest::getVlanId, v -> v));

        assertThat(networks.get(DefaultsPatcherV1Base.BACKBONE_VLAN).getVirtualServiceIdsList().containsAll(virtualServiceIds),
                equalTo(true));
        assertThat(networks.get(DefaultsPatcherV1Base.FASTBONE_VLAN).getVirtualServiceIdsList(), empty());
    }

    private void patchNetworksDefaultsScenario(
            TPodTemplateSpec.Builder podTemplateSpec,
            NetworkDefaults networkDefaults,
            int expectedIp6AddressRequestsSize,
            String expectedIp6AddressNetworkId,
            String expectedIp6SubnetNetworkId
    ) {
        var deployUnitContext = DEFAULT_UNIT_CONTEXT.withNetworkDefaults(networkDefaults);
        networkPatcherV3.patch(
                podTemplateSpec,
                deployUnitContext,
                ytBuilder
        );

        DataModel.TPodSpec podSpec = podTemplateSpec.getSpec();

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
        TPodTemplateSpec.Builder builder = TPodTemplateSpec.newBuilder();

        patchNetworksDefaultsScenario(
                builder,
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

        TPodTemplateSpec.Builder podTemplateSpecBuilder = TPodTemplateSpec.newBuilder();
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

}

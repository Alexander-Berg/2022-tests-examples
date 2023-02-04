package ru.yandex.infra.stage.podspecs.patcher.network;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTreeBuilder;
import ru.yandex.yp.client.api.DataModel;
import ru.yandex.yp.client.api.TPodTemplateSpec;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static ru.yandex.infra.stage.TestData.DEFAULT_UNIT_CONTEXT;
import static ru.yandex.infra.stage.podspecs.patcher.defaults.DefaultsPatcherV1Base.BACKBONE_VLAN;
import static ru.yandex.infra.stage.podspecs.patcher.defaults.DefaultsPatcherV1Base.FASTBONE_VLAN;
import static ru.yandex.infra.stage.podspecs.patcher.network.NetworkPatcherV1.NEW_BACKBONE_PRIORITY;

class NetworkPatcherV1Test {

    private static final String NETWORK_ID = "NETWORK_ID";
    private static final int DEFAULT_PRIORITY = 0;
    private YTreeBuilder labelsBuilder;

    @BeforeEach
    void beforeEach() {
        labelsBuilder = new YTreeBuilder();
    }

    @Test
    void testPatchingWithoutBackbone() {
        TPodTemplateSpec.Builder spec = TPodTemplateSpec.newBuilder();
        addIp6AddressRequests(spec, List.of(FASTBONE_VLAN));

        NetworkPatcherV1 networkPatcherV1 = new NetworkPatcherV1();
        networkPatcherV1.patch(spec, DEFAULT_UNIT_CONTEXT, labelsBuilder);
        // Nothing changed without backbone address
        assertThat(spec.getSpec().getIp6AddressRequestsCount(), equalTo(1));
        assertThat(getPriority(spec, FASTBONE_VLAN), equalTo(DEFAULT_PRIORITY));
    }

    @Test
    void testPatchingWithBackbone() {
        TPodTemplateSpec.Builder spec = TPodTemplateSpec.newBuilder();
        addIp6AddressRequests(spec, List.of(FASTBONE_VLAN, BACKBONE_VLAN));

        NetworkPatcherV1 networkPatcherV1 = new NetworkPatcherV1();
        networkPatcherV1.patch(spec, DEFAULT_UNIT_CONTEXT, labelsBuilder);
        // Priority for backbone address was patched
        assertThat(spec.getSpec().getIp6AddressRequestsCount(), equalTo(2));
        assertThat(getPriority(spec, FASTBONE_VLAN), equalTo(DEFAULT_PRIORITY));
        assertThat(getPriority(spec, BACKBONE_VLAN), equalTo(NEW_BACKBONE_PRIORITY));
    }

    private static DataModel.TPodSpec.TIP6AddressRequest makeIp6Request(String vlanId) {
        return DataModel.TPodSpec.TIP6AddressRequest.newBuilder()
                .setNetworkId(NETWORK_ID)
                .setVlanId(vlanId)
                .setEnableDns(true)
                .build();
    }

    private static void addIp6AddressRequests(TPodTemplateSpec.Builder builder, List<String> vlanIds) {
        builder.getSpecBuilder().addAllIp6AddressRequests(
                vlanIds.stream().map(NetworkPatcherV1Test::makeIp6Request).collect(Collectors.toList()));
    }

    private static int getPriority(TPodTemplateSpec.Builder spec, String vlanId) {
        return spec.getSpec().getIp6AddressRequestsList().stream()
                .filter(request -> request.getVlanId().equals(vlanId))
                .map(DataModel.TPodSpec.TIP6AddressRequest::getPriority)
                .findFirst()
                .get();
    }

}

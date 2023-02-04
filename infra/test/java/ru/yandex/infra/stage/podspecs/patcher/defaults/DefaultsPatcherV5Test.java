package ru.yandex.infra.stage.podspecs.patcher.defaults;

import java.util.function.Function;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.pods.EResolvConf;

public class DefaultsPatcherV5Test extends DefaultsPatcherV1BaseTest {

    @Override
    protected Function<DefaultsPatcherV1Context, ? extends SpecPatcher<TPodTemplateSpec.Builder>> getPatcherConstructor() {
        return DefaultsPatcherV5::new;
    }

    @ParameterizedTest
    @MethodSource("extraRoutesTestParametersFromV3ToLast")
    public void extraRoutesTest(EResolvConf conf, boolean expectedExtraRoutes) {
        extraRoutesScenario(conf, expectedExtraRoutes);
    }

    @ParameterizedTest
    @MethodSource("patchSoxLabelsTestParametersFromV4ToLast")
    void patchSoxLabelsTest(boolean isSoxService, String nodeFilter, String expectedNodeFilter) {
        patchSoxLabelsScenario(isSoxService, nodeFilter, expectedNodeFilter);
    }


    @Override
    void patchNetworksDefaultsWithIp4AddressPool() {
    }

    @Override
    void patchNetworksDefaultsWithVirtualServiceIds() {
    }

    @Override
    void patchNetworksAddressOverrideTest(boolean v1, int v2, String v3) {
    }

    @Override
    void patchNetworksDefaults() {
    }
}

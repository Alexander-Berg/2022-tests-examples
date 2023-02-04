package ru.yandex.infra.stage.podspecs.patcher.defaults;

import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.pods.EResolvConf;

public class DefaultsPatcherV2Test extends DefaultsPatcherV1BaseTest {

    @Override
    protected Function<DefaultsPatcherV1Context, ? extends SpecPatcher<TPodTemplateSpec.Builder>> getPatcherConstructor() {
        return DefaultsPatcherV2::new;
    }

    private static Stream<Arguments> extraRoutesTestParameters() {
        return extraRoutesTestParametersGenerator(false);
    }

    @ParameterizedTest
    @MethodSource("extraRoutesTestParameters")
    public void extraRoutesTest(EResolvConf conf, boolean expectedExtraRoutes) {
        extraRoutesScenario(conf, expectedExtraRoutes);
    }

    @ParameterizedTest
    @MethodSource("patchSoxLabelsTestParametersFromV1ToV3")
    void patchSoxLabelsTest(boolean isSoxService, String nodeFilter, String expectedNodeFilter) {
        patchSoxLabelsScenario(isSoxService, nodeFilter, expectedNodeFilter);
    }
}

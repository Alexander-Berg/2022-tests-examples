package ru.yandex.infra.stage.podspecs.patcher.defaults;

import java.util.function.Function;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.yp.client.api.Enums;
import ru.yandex.yp.client.api.TPodTemplateSpec;

public class DefaultsPatcherV1Test extends DefaultsPatcherV1BaseTest {

    @Override
    protected Function<DefaultsPatcherV1Context, ? extends SpecPatcher<TPodTemplateSpec.Builder>> getPatcherConstructor() {
        return DefaultsPatcherV1::new;
    }

    @Override
    protected TPodTemplateSpec.Builder createDefaultPodTemplateSpecBuilder() {
        return createDefaultPodTemplateSpecBuilderV1();
    }

    protected Enums.EPodHostNameKind getDefaultHostNameKind() {
        return Enums.EPodHostNameKind.PHNK_TRANSIENT;
    }

    @ParameterizedTest
    @MethodSource("patchSoxLabelsTestParametersFromV1ToV3")
    void patchSoxLabelsTest(boolean isSoxService, String nodeFilter, String expectedNodeFilter) {
        patchSoxLabelsScenario(isSoxService, nodeFilter, expectedNodeFilter);
    }
}

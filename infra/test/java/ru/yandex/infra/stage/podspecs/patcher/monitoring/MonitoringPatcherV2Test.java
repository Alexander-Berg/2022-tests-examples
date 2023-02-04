package ru.yandex.infra.stage.podspecs.patcher.monitoring;

import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.yp.client.api.TMonitoringUnistatEndpoint;
import ru.yandex.yp.client.api.TPodTemplateSpec;

public class MonitoringPatcherV2Test extends MonitoringPatcherV1BaseTest {

    @Override
    protected Function<MonitoringPatcherV1Context, ? extends SpecPatcher<TPodTemplateSpec.Builder>> getPatcherConstructor() {
        return MonitoringPatcherV2::new;
    }

    @Override
    protected boolean shouldAddEnvVars() {
        return true;
    }

    private static Stream<Arguments> setWorkloadLabelInUnistatTestParameters() {
        return setWorkloadLabelInUnistatTestParametersGenerator(false);
    }

    @ParameterizedTest
    @MethodSource("setWorkloadLabelInUnistatTestParameters")
    void setWorkloadLabelInUnistatTest(TMonitoringUnistatEndpoint unistatEndpoint,
                                       boolean mustContainWorkloadLabelIfWasAbsent) {
        setWorkloadLabelInUnistatScenario(unistatEndpoint, mustContainWorkloadLabelIfWasAbsent);
    }

    @Override
    boolean mustContainWorkloadKey() {
        return false;
    }
}

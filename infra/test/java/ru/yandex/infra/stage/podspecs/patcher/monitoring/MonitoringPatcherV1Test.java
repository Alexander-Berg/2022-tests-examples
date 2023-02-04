package ru.yandex.infra.stage.podspecs.patcher.monitoring;

import java.util.function.Function;

import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.yp.client.api.TPodTemplateSpec;

public class MonitoringPatcherV1Test extends MonitoringPatcherV1BaseTest {

    @Override
    protected Function<MonitoringPatcherV1Context, ? extends SpecPatcher<TPodTemplateSpec.Builder>> getPatcherConstructor() {
        return MonitoringPatcherV1::new;
    }

    @Override
    protected boolean shouldAddEnvVars() {
        return false;
    }

    @Override
    boolean mustContainWorkloadKey() {
        return false;
    }
}

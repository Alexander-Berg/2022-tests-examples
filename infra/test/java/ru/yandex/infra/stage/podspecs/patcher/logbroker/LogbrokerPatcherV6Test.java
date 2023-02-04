package ru.yandex.infra.stage.podspecs.patcher.logbroker;

import java.util.function.Function;

import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.yp.client.api.TPodTemplateSpec;

public class LogbrokerPatcherV6Test extends LogbrokerPatcherV1BaseTest {

    @Override
    protected Function<LogbrokerPatcherV1Context, ? extends SpecPatcher<TPodTemplateSpec.Builder>> getPatcherConstructor() {
        return LogbrokerPatcherV6::new;
    }

}

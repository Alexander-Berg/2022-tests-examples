package ru.yandex.infra.stage.podspecs.patcher.logbroker;

import java.util.function.Function;

import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.yp.client.api.TPodTemplateSpec;


public class LogbrokerPatcherV5Test extends LogbrokerPatcherV1BaseTest {

    @Override
    protected Function<LogbrokerPatcherV1Context, ? extends SpecPatcher<TPodTemplateSpec.Builder>> getPatcherConstructor() {
        return LogbrokerPatcherV5::new;
    }

    @Override
    protected boolean autoEnableSystemLogsWhenUserLogsEnabled(){
        return false;
    }

}

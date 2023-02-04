package ru.yandex.infra.stage.podspecs.patcher.logbroker;

import java.util.function.Function;

import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.yp.client.api.TPodTemplateSpec;

public class LogbrokerPatcherV3Test extends LogbrokerPatcherV1BaseTest {

    @Override
    protected Function<LogbrokerPatcherV1Context, ? extends SpecPatcher<TPodTemplateSpec.Builder>> getPatcherConstructor() {
        return LogbrokerPatcherV3::new;
    }

    @Override
    protected String getLogbrokerBoxId() {
        return LogbrokerPatcherUtils.LOGBROKER_BOX_ID_UNDERSCORED;
    }

    @Override
    protected boolean errorBoosterEnvironmentsExport() {
        return false;
    }

    @Override
    protected boolean errorBoosterHttpEnvironmentsExport() {
        return false;
    }

    @Override
    protected boolean autoEnableSystemLogsWhenUserLogsEnabled(){
        return false;
    }
}


package ru.yandex.infra.stage.podspecs.patcher.logrotate;

import java.util.function.Function;

import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.yp.client.api.TPodTemplateSpec;

public class LogrotatePatcherV1Test extends LogrotatePatcherV1BaseTest {
    @Override
    protected Function<LogrotatePatcherV1Context, ? extends SpecPatcher<TPodTemplateSpec.Builder>> getPatcherConstructor() {
        return LogrotatePatcherV1::new;
    }

    @Override
    protected boolean isLimited() {
        return false;
    }
}

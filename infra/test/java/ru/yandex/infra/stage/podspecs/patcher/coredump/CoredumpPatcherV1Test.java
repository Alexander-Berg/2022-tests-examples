package ru.yandex.infra.stage.podspecs.patcher.coredump;

import java.util.function.Function;

import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.yp.client.api.TPodTemplateSpec;


public class CoredumpPatcherV1Test extends CoredumpPatcherV1BaseTest {
    @Override
    protected Function<CoredumpPatcherV1Context, ? extends SpecPatcher<TPodTemplateSpec.Builder>> getPatcherConstructor() {
        return CoredumpPatcherV1::new;
    }
}

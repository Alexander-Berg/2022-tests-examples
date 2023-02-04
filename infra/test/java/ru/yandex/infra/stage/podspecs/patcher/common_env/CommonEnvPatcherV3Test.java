package ru.yandex.infra.stage.podspecs.patcher.common_env;

import java.util.function.Function;

import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.yp.client.api.TPodTemplateSpec;

public class CommonEnvPatcherV3Test extends CommonEnvPatcherV1BaseTest {

    @Override
    protected Function<CommonEnvPatcherV1Context, ? extends SpecPatcher<TPodTemplateSpec.Builder>> getPatcherConstructor() {
        return CommonEnvPatcherV3::new;
    }

    @Override
    protected boolean shouldAddResourceRequestEnvVars() {
        return true;
    }

    @Override
    protected boolean shouldAddPathVarInWorkflow() {
        return true;
    }
}

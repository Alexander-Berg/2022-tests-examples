package ru.yandex.infra.stage.podspecs.patcher.tvm;

import java.util.function.Function;

import org.junit.jupiter.api.Test;

import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.yp.client.api.TPodTemplateSpec;

public class TvmPatcherV4Test extends TvmPatcherV1BaseTest {
    @Override
    protected Function<TvmPatcherV1Context, ? extends SpecPatcher<TPodTemplateSpec.Builder>> getPatcherConstructor() {
        return TvmPatcherV4::new;
    }

    @Override
    protected String getTvmBoxId() {
        return TvmPatcherUtils.TVM_BOX_ID_UNDERSCORED;
    }

    @Test
    void addTvmConfigEnvVarTest() {
        addTvmConfigEnvVarScenario(true);
    }
}

package ru.yandex.infra.stage.podspecs.patcher.tvm;

import java.util.function.Function;

import ru.yandex.infra.stage.dto.TvmConfig;
import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.pods.TBox;

import static ru.yandex.infra.stage.podspecs.PodSpecUtils.MEGABYTE;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

public class TvmPatcherV1Test extends TvmPatcherV1BaseTest {
    @Override
    protected Function<TvmPatcherV1Context, ? extends SpecPatcher<TPodTemplateSpec.Builder>> getPatcherConstructor() {
        return TvmPatcherV1::new;
    }

    @Override
    protected void ensureTvmBoxLimits(TBox tvmBox) {
        var actualBoxResources = tvmBox.getComputeResources();
        assertThatEquals(actualBoxResources.getMemoryLimit(), TvmConfig.DEFAULT_MEMORY_LIMIT_MB * MEGABYTE);
        assertThatEquals((int) actualBoxResources.getVcpuLimit(), TvmConfig.DEFAULT_CPU_LIMIT);
    }

    @Override
    protected String getTvmBoxId() {
        return TvmPatcherUtils.TVM_BOX_ID_UNDERSCORED;
    }
}

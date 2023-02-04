package ru.yandex.infra.stage.podspecs.patcher.dynamic_resource;

import java.util.function.Function;

import ru.yandex.infra.stage.dto.AllComputeResources;
import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.pods.TBox;

public class DynamicResourcePatcherV3Test extends DynamicResourcePatcherV1BaseTest {
    @Override
    protected Function<DynamicResourcePatcherV1Context, ? extends SpecPatcher<TPodTemplateSpec.Builder>> getPatcherConstructor() {
        return DynamicResourcePatcherV3::new;
    }

    @Override
    protected AllComputeResources getExpectedAdditionalBoxResources() {
        return DynamicResourcePatcherV3.DRU_ADDITIONAL_RESOURCES;
    }

    @Override
    protected AllComputeResources getExpectedBoxGuaranteeResources(TBox boxSpec) {
        return getExpectedAdditionalBoxResources();
    }
}

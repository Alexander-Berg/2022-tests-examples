package ru.yandex.infra.stage.podspecs.patcher.dynamic_resource;

import java.util.function.Function;

import ru.yandex.infra.stage.dto.AllComputeResources;
import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.pods.TBox;


public class DynamicResourcePatcherV4Test extends DynamicResourcePatcherV1BaseTest {
    @Override
    protected Function<DynamicResourcePatcherV1Context, ? extends SpecPatcher<TPodTemplateSpec.Builder>> getPatcherConstructor() {
        return DynamicResourcePatcherV4::new;
    }

    @Override
    protected AllComputeResources getExpectedAdditionalBoxResources() {
        return DynamicResourcePatcherV4.DRU_ADDITIONAL_RESOURCES;
    }

    @Override
    protected AllComputeResources getExpectedBoxGuaranteeResources(TBox boxSpec) {
        var expectedResourcesBuilder = getExpectedAdditionalBoxResources().toBuilder();
        if (boxSpec.getComputeResources().getVcpuGuarantee() == 0) expectedResourcesBuilder.withVcpuGuarantee(0);
        if (boxSpec.getComputeResources().getMemoryGuarantee() == 0) expectedResourcesBuilder.withMemoryGuarantee(0);
        return expectedResourcesBuilder.build();
    }
}

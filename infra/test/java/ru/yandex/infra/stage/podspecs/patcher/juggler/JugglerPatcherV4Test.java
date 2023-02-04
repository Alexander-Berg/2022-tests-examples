package ru.yandex.infra.stage.podspecs.patcher.juggler;

import java.util.function.Function;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.infra.stage.dto.AllComputeResources;
import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.pods.TBox;

import static ru.yandex.infra.stage.podspecs.patcher.juggler.JugglerPatcherV1BaseTest.ComputeResourcesTestUtils.ExpectedDiskCapacityMode.JUGGLER_QUOTA_TO_USER;

public class JugglerPatcherV4Test extends JugglerPatcherV1BaseTest {

    @Override
    protected Function<JugglerPatcherV1Context, ? extends SpecPatcher<TPodTemplateSpec.Builder>> getPatcherConstructor() {
        return JugglerPatcherV4::new;
    }

    @Override
    protected AllComputeResources getExpectedAdditionalBoxResources() {
        return JugglerPatcherV4.JUGGLER_RESOURCE_PER_BOX;
    }

    @Override
    protected AllComputeResources getExpectedBoxGuaranteeResources(TBox boxSpec) {
        return getExpectedAdditionalBoxResources();
    }

    @ParameterizedTest
    @EnumSource(BoxAllocationMode.class)
    void workloadsResourcesTest(BoxAllocationMode boxAllocationMode) {
        ensureWorkloadsComputedResources(patchWithBoxesScenario(boxAllocationMode),
                JugglerPatcherV4.JUGGLER_RESOURCE_PER_BOX.toProto());
    }

    @Override
    protected ComputeResourcesTestUtils.ExpectedDiskCapacityMode getDiskCapacityMode() {
        return JUGGLER_QUOTA_TO_USER;
    }

}

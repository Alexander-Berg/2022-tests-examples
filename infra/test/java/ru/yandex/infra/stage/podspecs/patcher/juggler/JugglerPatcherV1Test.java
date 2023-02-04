package ru.yandex.infra.stage.podspecs.patcher.juggler;

import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.infra.stage.dto.AllComputeResources;
import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.yp.client.api.Enums;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.pods.TBox;
import ru.yandex.yp.client.pods.TComputeResources;

import static ru.yandex.infra.stage.podspecs.patcher.juggler.JugglerPatcherV1BaseTest.ComputeResourcesTestUtils.ExpectedDiskCapacityMode.JUGGLER_QUOTA_TO_SIDECAR;

public class JugglerPatcherV1Test extends JugglerPatcherV1BaseTest {

    @Override
    protected Function<JugglerPatcherV1Context, ? extends SpecPatcher<TPodTemplateSpec.Builder>> getPatcherConstructor() {
        return JugglerPatcherV1::new;
    }

    @Override
    protected AllComputeResources getExpectedAdditionalBoxResources() {
        return JugglerPatcherV1Base.JUGGLER_RESOURCE_PER_BOX_V1;
    }

    @Override
    protected AllComputeResources getExpectedBoxGuaranteeResources(TBox boxSpec) {
        return getExpectedAdditionalBoxResources();
    }

    @ParameterizedTest
    @EnumSource(BoxAllocationMode.class)
    void workloadsResourcesTest(BoxAllocationMode boxAllocationMode) {
        ensureWorkloadsComputedResources(patchWithBoxesScenario(boxAllocationMode),
                TComputeResources.getDefaultInstance());
    }

    private Enums.EPodHostNameKind getDefaultHostNameKind() {
        return Enums.EPodHostNameKind.PHNK_TRANSIENT;
    }

    @Test
    void setDefaultHostNameKindTest() {
        setHostNameKindScenario(createDefaultPodTemplateSpecBuilder(), getDefaultHostNameKind());
    }

    @Override
    protected ComputeResourcesTestUtils.ExpectedDiskCapacityMode getDiskCapacityMode() {
        return JUGGLER_QUOTA_TO_SIDECAR;
    }
}

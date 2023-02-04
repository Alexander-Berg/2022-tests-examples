package ru.yandex.infra.stage.podspecs.patcher.juggler;

import java.util.function.Function;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import ru.yandex.infra.stage.dto.AllComputeResources;
import ru.yandex.infra.stage.dto.BoxJugglerConfig;
import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.yp.client.api.TPodTemplateSpec;
import ru.yandex.yp.client.pods.TBox;


import static ru.yandex.infra.stage.podspecs.patcher.juggler.JugglerPatcherV1Base.createJugglerInitScript;
import static ru.yandex.infra.stage.podspecs.patcher.juggler.JugglerPatcherV1BaseTest.ComputeResourcesTestUtils.ExpectedDiskCapacityMode.JUGGLER_QUOTA_TO_USER;
import static ru.yandex.infra.stage.podspecs.patcher.juggler.JugglerPatcherV6.TEMPLATE_START_COMMAND_V6;

public class JugglerPatcherV7Test extends JugglerPatcherV1BaseTest {

    @Override
    protected Function<JugglerPatcherV1Context, ? extends SpecPatcher<TPodTemplateSpec.Builder>> getPatcherConstructor() {
        return JugglerPatcherV7::new;
    }

    @Override
    protected AllComputeResources getExpectedAdditionalBoxResources() {
        return JugglerPatcherV4.JUGGLER_RESOURCE_PER_BOX;
    }

    @Override
    protected AllComputeResources getExpectedBoxGuaranteeResources(TBox boxSpec) {
        var expectedResourcesBuilder = getExpectedAdditionalBoxResources().toBuilder();
        if (boxSpec.getComputeResources().getVcpuGuarantee() == 0) expectedResourcesBuilder.withVcpuGuarantee(0);
        if (boxSpec.getComputeResources().getMemoryGuarantee() == 0) expectedResourcesBuilder.withMemoryGuarantee(0);
        return expectedResourcesBuilder.build();
    }

    @ParameterizedTest
    @EnumSource(BoxAllocationMode.class)
    void workloadsResourcesTest(BoxAllocationMode boxAllocationMode) {
        ensureWorkloadsComputedResources(patchWithBoxesScenario(boxAllocationMode),
                JugglerPatcherV4.JUGGLER_RESOURCE_PER_BOX.toProto());
    }

    @ParameterizedTest
    @EnumSource(BoxAllocationMode.class)
    void jugglerWorkloadsStartCommandTest(BoxAllocationMode boxAllocationMode) {
        ensureJugglerWorkloadsStartCommand(patchWithBoxesScenario(boxAllocationMode),
                String.format(TEMPLATE_START_COMMAND_V6, BoxJugglerConfig.DEFAULT_PORT));
    }

    @ParameterizedTest
    @EnumSource(BoxAllocationMode.class)
    void boxesWithJugglerInitCommandTest(BoxAllocationMode boxAllocationMode) {
        ensureBoxesWithJugglerInitCommand(patchWithBoxesScenario(boxAllocationMode),
                String.format("/bin/bash -c '%s'", createJugglerInitScript("juggler_init_template_v4.sh")));
    }

    @Override
    protected ComputeResourcesTestUtils.ExpectedDiskCapacityMode getDiskCapacityMode() {
        return JUGGLER_QUOTA_TO_USER;
    }
}

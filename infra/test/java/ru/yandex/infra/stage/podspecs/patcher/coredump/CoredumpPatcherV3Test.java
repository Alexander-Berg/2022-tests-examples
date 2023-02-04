package ru.yandex.infra.stage.podspecs.patcher.coredump;

import java.util.Optional;
import java.util.function.Function;

import ru.yandex.infra.stage.dto.CoredumpConfig;
import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.yp.client.api.TPodTemplateSpec;

public class CoredumpPatcherV3Test extends CoredumpPatcherV1BaseTest {

    @Override
    protected Function<CoredumpPatcherV1Context, ? extends SpecPatcher<TPodTemplateSpec.Builder>> getPatcherConstructor() {
        return CoredumpPatcherV3::new;
    }

    @Override
    protected String expectedItype(String stageId, String workloadId, Optional<String> monitoringWorkloadItype, Optional<String> monitoringItype,
                                   CoredumpConfig coredumpConfig) {
        return coredumpConfig.getServiceName().orElse(monitoringWorkloadItype.orElse(monitoringItype.orElse(CoredumpPatcherV1Base.getDefaultItype(stageId, workloadId))));
    }


}

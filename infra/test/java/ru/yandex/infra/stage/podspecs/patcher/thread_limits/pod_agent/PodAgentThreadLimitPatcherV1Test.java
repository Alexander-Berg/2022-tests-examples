package ru.yandex.infra.stage.podspecs.patcher.thread_limits.pod_agent;

import java.util.function.Function;

import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.yp.client.api.TPodTemplateSpec;

public class PodAgentThreadLimitPatcherV1Test extends PodAgentThreadLimitPatcherV1BaseTest {

    @Override
    protected Function<PodAgentThreadLimitPatcherV1Context, ? extends SpecPatcher<TPodTemplateSpec.Builder>> getPatcherConstructor() {
        return PodAgentThreadLimitPatcherV1::new;
    }
}

package ru.yandex.infra.stage;

import org.junit.jupiter.api.Test;

import ru.yandex.infra.controller.util.ResourceUtils;
import ru.yandex.infra.stage.dto.PushAgentConfig;
import ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherUtils;
import ru.yandex.infra.stage.util.AssertUtils;

class PushAgentConfigTest {
    @Test
    void generateJsonConfig() {
        String json = new PushAgentConfig("stage_id", LogbrokerPatcherUtils.LOGBROKER_DEFAULT_STATIC_SECRET)
                .toJsonString();
        String expectedJson = ResourceUtils.readResource("pushagent.conf");
        AssertUtils.assertJsonStringEquals(json, expectedJson);
    }
}

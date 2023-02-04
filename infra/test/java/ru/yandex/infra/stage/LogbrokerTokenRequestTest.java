package ru.yandex.infra.stage;

import org.junit.jupiter.api.Test;

import ru.yandex.infra.controller.util.ResourceUtils;
import ru.yandex.infra.stage.dto.LogbrokerDelegationTokenRequest;
import ru.yandex.infra.stage.util.AssertUtils;

class LogbrokerTokenRequestTest {
    @Test
    void generateJsonConfig() {
        String json = new LogbrokerDelegationTokenRequest("full_deploy_unit_id", 2001151L)
                .toJsonString();

        String expectedJson = ResourceUtils.readResource("delegation_token_request");
        AssertUtils.assertJsonStringEquals(json, expectedJson);
    }
}

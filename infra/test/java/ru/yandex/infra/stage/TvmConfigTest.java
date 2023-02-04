package ru.yandex.infra.stage;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import ru.yandex.infra.controller.util.ResourceUtils;
import ru.yandex.infra.stage.util.AssertUtils;

class TvmConfigTest {
    @Test
    void generateJsonConfig() {
        String json = TestData.TVM_CONFIG.toJsonString(ImmutableMap.of("test", 1));
        String expectedJson = ResourceUtils.readResource("tvmtool.conf");
        AssertUtils.assertJsonStringEquals(json, expectedJson);
    }

    @Test
    void generateJsonConfigForCheckOnlyMode() {
        //"Check only" == no destinations
        String json = TestData.TVM_CONFIG_WITH_NO_DESTINATIONS.toJsonString(ImmutableMap.of("test", 1));
        String expectedJson = ResourceUtils.readResource("tvmtool_checkonly.conf");
        AssertUtils.assertJsonStringEquals(json, expectedJson);
    }
}

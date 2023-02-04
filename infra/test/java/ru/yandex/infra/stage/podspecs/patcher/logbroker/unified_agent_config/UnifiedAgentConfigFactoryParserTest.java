package ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config;

import java.util.stream.Stream;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherConfigParser.UNIFIED_AGENT_CONFIG_FACTORY_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherConfigParserTest.CORRECT_LOGBROKER_PATCHER_CONFIG;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.UnifiedAgentConfigFactoryParser.DATA_RETENTION_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.UnifiedAgentConfigFactoryParser.ERROR_BOOSTER_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.UnifiedAgentConfigFactoryParser.THROTTLING_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.UnifiedAgentConfigFactoryParser.VERSION_CALCULATOR_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.UnifiedAgentConfigFactoryParserTestExpectedValues.EXPECTED_UNIFIED_AGENT_CONFIG_FACTORY;

public class UnifiedAgentConfigFactoryParserTest {

    static final Config CORRECT_UNIFIED_AGENT_CONFIG_FACTORY_CONFIG = CORRECT_LOGBROKER_PATCHER_CONFIG.getConfig(
            UNIFIED_AGENT_CONFIG_FACTORY_CONFIG_PATH
    );

    @Test
    void parseConfigTest() {
        var actualPatcherConfig = UnifiedAgentConfigFactoryParser.parseConfig(
                CORRECT_UNIFIED_AGENT_CONFIG_FACTORY_CONFIG
        );

        assertThat(actualPatcherConfig, equalTo(EXPECTED_UNIFIED_AGENT_CONFIG_FACTORY));
    }


    private static Stream<Config> parseConfigIncorrectTestParameters() {
        return Stream.of(
                THROTTLING_CONFIG_PATH,
                ERROR_BOOSTER_CONFIG_PATH,
                VERSION_CALCULATOR_CONFIG_PATH,
                DATA_RETENTION_CONFIG_PATH
        ).map(CORRECT_UNIFIED_AGENT_CONFIG_FACTORY_CONFIG::withoutPath);
    }

    @ParameterizedTest
    @MethodSource("parseConfigIncorrectTestParameters")
    public void parseConfigIncorrectTest(Config incorrectConfig) {
        assertThrows(
                ConfigException.Missing.class,
                () -> UnifiedAgentConfigFactoryParser.parseConfig(incorrectConfig)
        );
    }
}

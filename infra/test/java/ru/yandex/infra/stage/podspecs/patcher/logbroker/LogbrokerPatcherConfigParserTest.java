package ru.yandex.infra.stage.podspecs.patcher.logbroker;

import java.util.stream.Stream;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.infra.stage.ConfigUtils;
import ru.yandex.infra.stage.podspecs.patcher.TestWithPatcherConfigs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerBoxResourcesConfigParserTestExpectedValues.EXPECTED_BOX_RESOURCES_CONFIG;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherConfigParser.BOX_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherConfigParser.UNIFIED_AGENT_CONFIG_FACTORY_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.UnifiedAgentConfigFactoryParserTestExpectedValues.EXPECTED_UNIFIED_AGENT_CONFIG_FACTORY;

public class LogbrokerPatcherConfigParserTest extends TestWithPatcherConfigs {

    public static final Config CORRECT_LOGBROKER_PATCHER_CONFIG = ConfigUtils.logbrokerPatcherConfig(
            CORRECT_PATCHERS_CONFIG
    );

    private static final LogbrokerPatcherConfig EXPECTED_PATCHER_CONFIG = new LogbrokerPatcherConfig(
            EXPECTED_UNIFIED_AGENT_CONFIG_FACTORY, EXPECTED_BOX_RESOURCES_CONFIG
    );

    @Test
    void parseConfigTest() {
        var actualPatcherConfig = LogbrokerPatcherConfigParser.parseConfig(
                CORRECT_LOGBROKER_PATCHER_CONFIG
        );

        assertThat(actualPatcherConfig, equalTo(EXPECTED_PATCHER_CONFIG));
    }


    private static Stream<Config> parseConfigIncorrectTestParameters() {
        return Stream.of(
                UNIFIED_AGENT_CONFIG_FACTORY_CONFIG_PATH,
                BOX_CONFIG_PATH
        ).map(CORRECT_LOGBROKER_PATCHER_CONFIG::withoutPath);
    }

    @ParameterizedTest
    @MethodSource("parseConfigIncorrectTestParameters")
    public void parseConfigIncorrectTest(Config incorrectConfig) {
        assertThrows(
                ConfigException.Missing.class,
                () -> LogbrokerPatcherConfigParser.parseConfig(incorrectConfig)
        );
    }
}

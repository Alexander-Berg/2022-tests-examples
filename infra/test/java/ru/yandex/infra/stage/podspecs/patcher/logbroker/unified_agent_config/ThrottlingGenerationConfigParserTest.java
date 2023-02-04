package ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config;

import java.util.Map;
import java.util.stream.Stream;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.infra.stage.ConfigUtils;
import ru.yandex.infra.stage.util.NamedArgument;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.ThrottlingGenerationConfigParser.THROTTLING_FIRST_AFFECTED_UNIFIED_AGENT_VERSION_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.ThrottlingGenerationConfigParser.THROTTLING_PATCHER_LIMITS_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.ThrottlingGenerationConfigParser.THROTTLING_WHITE_LIST_FILE_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.ThrottlingGenerationConfigParserTestExpectedValues.EXPECTED_THROTTLING_CONFIG;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.UnifiedAgentConfigFactoryParser.THROTTLING_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.UnifiedAgentConfigFactoryParserTest.CORRECT_UNIFIED_AGENT_CONFIG_FACTORY_CONFIG;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

public class ThrottlingGenerationConfigParserTest {

    private static final Config CORRECT_THROTTLING_CONFIG = CORRECT_UNIFIED_AGENT_CONFIG_FACTORY_CONFIG.getConfig(
            THROTTLING_CONFIG_PATH
    );

    private static Stream<Arguments> parseThrottlingConfigTestParameters() {
        var correctThrottlingConfig = CORRECT_THROTTLING_CONFIG;

        var emptyWhiteListFilePath = ConfigUtils.withValue(
                correctThrottlingConfig,
                THROTTLING_WHITE_LIST_FILE_CONFIG_PATH,
                ""
        );

        var withoutWhiteListFilePath = correctThrottlingConfig.withoutPath(
                THROTTLING_WHITE_LIST_FILE_CONFIG_PATH
        );

        var configWithEmptyDeployUnitLimitsArgument = NamedArgument.of(
                "empty deploy unit limits",
                EXPECTED_THROTTLING_CONFIG.toBuilder()
                        .withDeployUnitLimits(emptyMap())
                        .build()
        );

        return Stream.of(
                Arguments.of(
                        NamedArgument.of("default", correctThrottlingConfig),
                        NamedArgument.of("default", EXPECTED_THROTTLING_CONFIG)
                ),
                Arguments.of(
                        NamedArgument.of("empty white list file path", emptyWhiteListFilePath),
                        configWithEmptyDeployUnitLimitsArgument
                ),
                Arguments.of(
                        NamedArgument.of("without white list file path", withoutWhiteListFilePath),
                        configWithEmptyDeployUnitLimitsArgument
                )
        );
    }

    @ParameterizedTest
    @MethodSource("parseThrottlingConfigTestParameters")
    void parseConfigTest(NamedArgument<Config> throttlingConfig,
                         NamedArgument<ThrottlingGenerationConfig> expectedThrottlingConfig) {
        var actualThrottlingConfig = ThrottlingGenerationConfigParser.parseConfig(
                throttlingConfig.getArgument()
        );

        assertThatEquals(actualThrottlingConfig, expectedThrottlingConfig.getArgument());
    }

    @ParameterizedTest
    @ValueSource(strings = {"not_found_file.conf", "throttling_white_list_incorrect.conf"})
    void parseIncorrectThrottlingWhiteListFileTest(String incorrectThrottlingWhiteListFile) {
        var throttlingConfig = ConfigUtils.withValue(
                CORRECT_THROTTLING_CONFIG,
                THROTTLING_WHITE_LIST_FILE_CONFIG_PATH,
                incorrectThrottlingWhiteListFile
        );

        assertThrows(
                Exception.class,
                () -> ThrottlingGenerationConfigParser.parseConfig(throttlingConfig)
        );
    }

    private static Stream<Config> parseConfigIncorrectTestParameters() {
        return Stream.of(
                THROTTLING_FIRST_AFFECTED_UNIFIED_AGENT_VERSION_CONFIG_PATH,
                THROTTLING_PATCHER_LIMITS_CONFIG_PATH
        ).map(CORRECT_THROTTLING_CONFIG::withoutPath);
    }

    @ParameterizedTest
    @MethodSource("parseConfigIncorrectTestParameters")
    public void parseConfigIncorrectTest(Config incorrectConfig) {
        assertThrows(
                ConfigException.Missing.class,
                () -> ThrottlingGenerationConfigParser.parseConfig(incorrectConfig)
        );
    }
}

class ThrottlingGenerationConfigParserTestExpectedValues {

    private static final Map<String, ThrottlingLimits> EXPECTED_DEPLOY_UNIT_LIMITS = Map.of(
            "test_stage_1.du_1", new ThrottlingLimits("550kb", 50000),
            "test_stage_1.du_2", new ThrottlingLimits("1gb", 3000),
            "test_stage_2.du_1", new ThrottlingLimits("600mb", 1000)
    );

    private static final long EXPECTED_FIRST_AFFECTED_UNIFIED_AGENT_VERSION = 123456789;

    private static final Map<String, ThrottlingLimits> EXPECTED_PATCHER_LIMITS = Map.of(
            "limits_from_patcher_v0_to_v5", new ThrottlingLimits("10mb", 12345),
            "limits_from_patcher_v6_to_last", new ThrottlingLimits("30kb", 9876)
    );

    static final ThrottlingGenerationConfig EXPECTED_THROTTLING_CONFIG = new ThrottlingGenerationConfig(
            EXPECTED_DEPLOY_UNIT_LIMITS,
            EXPECTED_FIRST_AFFECTED_UNIFIED_AGENT_VERSION,
            EXPECTED_PATCHER_LIMITS
    );
}

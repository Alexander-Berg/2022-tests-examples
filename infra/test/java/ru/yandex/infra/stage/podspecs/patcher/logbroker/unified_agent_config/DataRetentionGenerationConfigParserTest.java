package ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.infra.stage.ConfigUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.DataRetentionGenerationConfigParser.DATA_RETENTION_DEFAULT_LIMITS_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.DataRetentionGenerationConfigParser.DATA_RETENTION_ENABLED_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.DataRetentionGenerationConfigParser.DATA_RETENTION_LIMITS_AGE_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.DataRetentionGenerationConfigParser.DATA_RETENTION_LIMITS_SIZE_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.DataRetentionGenerationConfigParserTestExpectedValues.EXPECTED_DATA_RETENTION_CONFIG;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.DataRetentionGenerationConfigParserTestExpectedValues.EXPECTED_DEFAULT_AGE_LIMIT;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.DataRetentionGenerationConfigParserTestExpectedValues.EXPECTED_DEFAULT_SIZE_LIMIT;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.UnifiedAgentConfigFactoryParser.DATA_RETENTION_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.UnifiedAgentConfigFactoryParserTest.CORRECT_UNIFIED_AGENT_CONFIG_FACTORY_CONFIG;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

public class DataRetentionGenerationConfigParserTest {
    private static final Config CORRECT_DATA_RETENTION_CONFIG = CORRECT_UNIFIED_AGENT_CONFIG_FACTORY_CONFIG.getConfig(
            DATA_RETENTION_CONFIG_PATH
    );


    private static void parseConfigScenario(
            Config dataRetentionConfig,
            DataRetentionGenerationConfig expectedDataRetentionConfig
    ) {
        var actualDataRetentionConfig = DataRetentionGenerationConfigParser.parseConfig(
                dataRetentionConfig
        );

        assertThatEquals(actualDataRetentionConfig, expectedDataRetentionConfig);
    }

    @Test
    public void parseConfigCorrectTest() {
        parseConfigScenario(
                CORRECT_DATA_RETENTION_CONFIG,
                EXPECTED_DATA_RETENTION_CONFIG
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            DATA_RETENTION_ENABLED_CONFIG_PATH,
            DATA_RETENTION_DEFAULT_LIMITS_CONFIG_PATH
    })
    public void parseConfigMissingTest(String missingPath) {
        assertThrows(
                ConfigException.Missing.class,
                () -> DataRetentionGenerationConfigParser.parseConfig(
                        CORRECT_DATA_RETENTION_CONFIG.withoutPath(missingPath)
                )
        );
    }

    private static Stream<Arguments> parseDataRetentionDefaultLimitsTestParameters() {
        var argumentsBuilder = Stream.<Arguments>builder();

        var correctDefaultLimitsConfig = CORRECT_DATA_RETENTION_CONFIG.getConfig(
                DATA_RETENTION_DEFAULT_LIMITS_CONFIG_PATH
        );

        var possibleAgeLimits = List.<Optional<String>>of(
                Optional.empty(),
                Optional.of(EXPECTED_DEFAULT_AGE_LIMIT)
        );

        var possibleSizeLimits = List.<Optional<String>>of(
                Optional.empty(),
                Optional.of(EXPECTED_DEFAULT_SIZE_LIMIT)
        );

        for (var ageLimit : possibleAgeLimits) {
            for (var sizeLimit : possibleSizeLimits) {
                var defaultLimitsConfig = correctDefaultLimitsConfig;

                defaultLimitsConfig = ConfigUtils.withValue(
                        defaultLimitsConfig,
                        DATA_RETENTION_LIMITS_AGE_CONFIG_PATH,
                        ageLimit
                );

                defaultLimitsConfig = ConfigUtils.withValue(
                        defaultLimitsConfig,
                        DATA_RETENTION_LIMITS_SIZE_CONFIG_PATH,
                        sizeLimit
                );

                var expectedDefaultLimitsBuilder = DataRetentionLimits.builder();
                ageLimit.ifPresent(expectedDefaultLimitsBuilder::withAgeLimit);
                sizeLimit.ifPresent(expectedDefaultLimitsBuilder::withSizeLimit);

                argumentsBuilder.add(
                        Arguments.of(
                                defaultLimitsConfig,
                                expectedDefaultLimitsBuilder.build()
                        )
                );
            }
        }

        return argumentsBuilder.build();
    }

    @ParameterizedTest
    @MethodSource("parseDataRetentionDefaultLimitsTestParameters")
    void parseDataRetentionDefaultLimitsTest(Config defaultLimitsConfig,
                                             DataRetentionLimits expectedDefaultLimits) {
        var dataRetentionConfig = CORRECT_DATA_RETENTION_CONFIG.withValue(
                DATA_RETENTION_DEFAULT_LIMITS_CONFIG_PATH,
                defaultLimitsConfig.root()
        );

        var expectedDataRetentionConfig = EXPECTED_DATA_RETENTION_CONFIG.toBuilder()
                .withDefaultLimits(expectedDefaultLimits)
                .build();

        parseConfigScenario(
                dataRetentionConfig,
                expectedDataRetentionConfig
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void parseDataRetentionEnabledTest(boolean enabled) {
        var dataRetentionConfig = ConfigUtils.withValue(
                CORRECT_DATA_RETENTION_CONFIG,
                DATA_RETENTION_ENABLED_CONFIG_PATH,
                enabled
        );

        var expectedDataRetentionConfig = EXPECTED_DATA_RETENTION_CONFIG.toBuilder()
                .withEnabled(enabled)
                .build();

        parseConfigScenario(
                dataRetentionConfig,
                expectedDataRetentionConfig
        );
    }
}

class DataRetentionGenerationConfigParserTestExpectedValues {

    private static final boolean EXPECTED_ENABLED = true;

    static final String EXPECTED_DEFAULT_AGE_LIMIT = "7d";
    static final String EXPECTED_DEFAULT_SIZE_LIMIT = "100mb";

    private static final DataRetentionLimits EXPECTED_DEFAULT_LIMITS = new DataRetentionLimits(
            EXPECTED_DEFAULT_AGE_LIMIT,
            EXPECTED_DEFAULT_SIZE_LIMIT
    );

    static final DataRetentionGenerationConfig EXPECTED_DATA_RETENTION_CONFIG = new DataRetentionGenerationConfig(
            EXPECTED_ENABLED,
            EXPECTED_DEFAULT_LIMITS
    );
}

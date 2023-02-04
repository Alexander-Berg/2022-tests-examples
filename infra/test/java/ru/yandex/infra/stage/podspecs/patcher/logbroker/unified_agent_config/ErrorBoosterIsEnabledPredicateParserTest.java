package ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config;

import java.util.Optional;
import java.util.stream.Stream;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.infra.stage.ConfigUtils;
import ru.yandex.infra.stage.util.NamedArgument;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.ErrorBoosterGenerationConfigParserTestExpectedValues.EXPECTED_ERROR_BOOSTER_GENERATION_CONFIG;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.ErrorBoosterIsEnabledPredicateParser.ERROR_BOOSTER_ENABLED_IN_RELEASE_FLAG_NAME_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.ErrorBoosterIsEnabledPredicateParser.ERROR_BOOSTER_FIRST_AFFECTED_UNIFIED_AGENT_VERSION_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.UnifiedAgentConfigFactoryParser.ERROR_BOOSTER_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.UnifiedAgentConfigFactoryParserTest.CORRECT_UNIFIED_AGENT_CONFIG_FACTORY_CONFIG;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

public class ErrorBoosterIsEnabledPredicateParserTest {
    private static final Config CORRECT_ERROR_BOOSTER_CONFIG = CORRECT_UNIFIED_AGENT_CONFIG_FACTORY_CONFIG.getConfig(
            ERROR_BOOSTER_CONFIG_PATH
    );

    private static Stream<Arguments> parseErrorBoosterConfigTestParameters() {
        var correctErrorBoosterConfig = CORRECT_ERROR_BOOSTER_CONFIG;

        var emptyEnabledInReleaseFlagName = ConfigUtils.withValue(
                correctErrorBoosterConfig,
                ERROR_BOOSTER_ENABLED_IN_RELEASE_FLAG_NAME_CONFIG_PATH,
                ""
        );

        var withoutEnabledInReleaseFlagName = correctErrorBoosterConfig.withoutPath(
                ERROR_BOOSTER_ENABLED_IN_RELEASE_FLAG_NAME_CONFIG_PATH
        );

        var configWithEmptyEnabledInReleaseFlagName = EXPECTED_ERROR_BOOSTER_GENERATION_CONFIG.toBuilder()
                .withEnabledInReleaseFlagName(Optional.empty())
                .build();

        var configWithEmptyEnabledInReleaseFlagNameArgument = NamedArgument.of(
                "empty enabled in release flag name",
                configWithEmptyEnabledInReleaseFlagName
        );

        return Stream.of(
                Arguments.of(
                        NamedArgument.of("default", correctErrorBoosterConfig),
                        NamedArgument.of("default", EXPECTED_ERROR_BOOSTER_GENERATION_CONFIG)
                ),
                Arguments.of(
                        NamedArgument.of("empty enabled in release flag name", emptyEnabledInReleaseFlagName),
                        configWithEmptyEnabledInReleaseFlagNameArgument
                ),
                Arguments.of(
                        NamedArgument.of("without enabled in release flag name", withoutEnabledInReleaseFlagName),
                        configWithEmptyEnabledInReleaseFlagNameArgument
                )
        );
    }

    @ParameterizedTest
    @MethodSource("parseErrorBoosterConfigTestParameters")
    void parseConfigTest(NamedArgument<Config> errorBoosterIsEnabledPredicate,
                         NamedArgument<ErrorBoosterIsEnabledPredicate> expectedErrorBoosterGenerationConfig) {
        var actualErrorBoosterGenerationConfig = ErrorBoosterIsEnabledPredicateParser.parseConfig(
                errorBoosterIsEnabledPredicate.getArgument()
        );

        assertThatEquals(actualErrorBoosterGenerationConfig, expectedErrorBoosterGenerationConfig.getArgument());
    }

    private static Stream<Config> parseConfigIncorrectTestParameters() {
        return Stream.of(
                ERROR_BOOSTER_FIRST_AFFECTED_UNIFIED_AGENT_VERSION_CONFIG_PATH
        ).map(CORRECT_ERROR_BOOSTER_CONFIG::withoutPath);
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

class ErrorBoosterGenerationConfigParserTestExpectedValues {

    private static final long EXPECTED_FIRST_AFFECTED_UNIFIED_AGENT_VERSION = 234567891;

    private static final String EXPECTED_ENABLED_IN_RELEASE_FLAG_NAME = "yd_error_booster_enabled";

    static final ErrorBoosterIsEnabledPredicateImpl EXPECTED_ERROR_BOOSTER_GENERATION_CONFIG = new ErrorBoosterIsEnabledPredicateImpl(
            EXPECTED_FIRST_AFFECTED_UNIFIED_AGENT_VERSION,
            Optional.of(EXPECTED_ENABLED_IN_RELEASE_FLAG_NAME)
    );
}

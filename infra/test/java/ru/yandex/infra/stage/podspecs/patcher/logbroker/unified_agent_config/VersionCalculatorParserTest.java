package ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.infra.stage.ConfigUtils;
import ru.yandex.infra.stage.util.NamedArgument;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.UnifiedAgentConfigFactoryParser.VERSION_CALCULATOR_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.UnifiedAgentConfigFactoryParserTest.CORRECT_UNIFIED_AGENT_CONFIG_FACTORY_CONFIG;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.VersionCalculatorParser.DEFAULT_VERSION_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.VersionCalculatorParser.VERSION_TO_FIRST_AFFECTED_UNIFIED_AGENT_VERSION_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.VersionCalculatorParserTestExpectedValues.EXPECTED_VERSION_CALCULATOR;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

public class VersionCalculatorParserTest {
    private static final Config CORRECT_VERSION_CALCULATOR_CONFIG = CORRECT_UNIFIED_AGENT_CONFIG_FACTORY_CONFIG.getConfig(
            VERSION_CALCULATOR_CONFIG_PATH
    );

    private static Stream<Arguments> parseConfigTestParameters() {
        return Stream.of(
                Arguments.of(
                        NamedArgument.of("default", CORRECT_VERSION_CALCULATOR_CONFIG),
                        NamedArgument.of("default", EXPECTED_VERSION_CALCULATOR)
                )
        );
    }

    @ParameterizedTest
    @MethodSource("parseConfigTestParameters")
    void parseConfigTest(NamedArgument<Config> versionCalculatorConfig,
                         NamedArgument<ThrottlingGenerationConfig> expectedVersionCalculator) {
        var actualVersionCalculator = VersionCalculatorParser.parseConfig(
                versionCalculatorConfig.getArgument()
        );

        assertThatEquals(actualVersionCalculator, expectedVersionCalculator.getArgument());
    }

    private static Stream<Arguments> parseConfigIncorrectTestParameters() {
        var arguments = Stream.<Arguments>builder();

        var requiredConfigsPaths = List.of(
                VERSION_TO_FIRST_AFFECTED_UNIFIED_AGENT_VERSION_CONFIG_PATH,
                DEFAULT_VERSION_CONFIG_PATH
        );

        requiredConfigsPaths.stream()
                .map(path ->
                        NamedArgument.of(
                                "without " + path,
                                CORRECT_VERSION_CALCULATOR_CONFIG.withoutPath(path)
                        )
                )
                .map(config -> Arguments.of(config, ConfigException.Missing.class))
                .forEach(arguments::add);

        String incorrectVersionString = "incorrect version";

        var withIncorrectVersionStringKey = ConfigUtils.withValue(
                CORRECT_VERSION_CALCULATOR_CONFIG,
                VERSION_TO_FIRST_AFFECTED_UNIFIED_AGENT_VERSION_CONFIG_PATH,
                Map.of(incorrectVersionString, 33)
        );

        arguments.add(Arguments.of(
                NamedArgument.of(
                        "with incorrect version key in version_to_ua_version",
                        withIncorrectVersionStringKey
                ),
                IllegalArgumentException.class
        ));

        var withIncorrectUnifiedAgentVersion = ConfigUtils.withValue(
                CORRECT_VERSION_CALCULATOR_CONFIG,
                VERSION_TO_FIRST_AFFECTED_UNIFIED_AGENT_VERSION_CONFIG_PATH,
                Map.of("v1", "incorrect unified agent version")
        );

        arguments.add(Arguments.of(
                NamedArgument.of(
                        "with incorrect ua version",
                        withIncorrectUnifiedAgentVersion
                ),
                ConfigException.WrongType.class
        ));

        var withIncorrectDefaultVersionString = ConfigUtils.withValue(
                CORRECT_VERSION_CALCULATOR_CONFIG,
                DEFAULT_VERSION_CONFIG_PATH,
                incorrectVersionString
        );

        arguments.add(Arguments.of(
                NamedArgument.of(
                        "incorrect default version",
                        withIncorrectDefaultVersionString
                ),
                IllegalArgumentException.class
        ));

        return arguments.build();
    }

    @ParameterizedTest
    @MethodSource("parseConfigIncorrectTestParameters")
    public void parseConfigIncorrectTest(
            NamedArgument<Config> incorrectConfig,
            Class<? extends Exception> expectedExceptionClass
    ) {
        assertThrows(
                expectedExceptionClass,
                () -> VersionCalculatorParser.parseConfig(incorrectConfig.getArgument())
        );
    }
}

class VersionCalculatorParserTestExpectedValues {

    private static final Map<UnifiedAgentConfigVersion, Long> EXPECTED_VERSION_TO_FIRST_AFFECTED_UNIFIED_AGENT_VERSION = Map.of(
            UnifiedAgentConfigVersion.V1, 23L,
            UnifiedAgentConfigVersion.V2, 47L
    );

    private static final UnifiedAgentConfigVersion EXPECTED_DEFAULT_VERSION = UnifiedAgentConfigVersion.V2;

    static final VersionCalculator EXPECTED_VERSION_CALCULATOR = new VersionCalculatorImpl(
            EXPECTED_VERSION_TO_FIRST_AFFECTED_UNIFIED_AGENT_VERSION,
            EXPECTED_DEFAULT_VERSION
    );
}

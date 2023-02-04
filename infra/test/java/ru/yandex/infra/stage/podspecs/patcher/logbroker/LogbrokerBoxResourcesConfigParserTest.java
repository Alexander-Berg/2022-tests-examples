package ru.yandex.infra.stage.podspecs.patcher.logbroker;

import java.util.Map;
import java.util.stream.Stream;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.infra.stage.dto.AllComputeResources;
import ru.yandex.infra.stage.podspecs.PodSpecUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.infra.stage.config.WhiteListConfigParser.WHITE_LIST_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerBoxResourcesConfigParser.RESOURCES_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerBoxResourcesConfigParserTestExpectedValues.EXPECTED_BOX_RESOURCES_CONFIG;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherConfigParser.BOX_CONFIG_PATH;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.LogbrokerPatcherConfigParserTest.CORRECT_LOGBROKER_PATCHER_CONFIG;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

public class LogbrokerBoxResourcesConfigParserTest {

    private static final Config CORRECT_BOX_CONFIG = CORRECT_LOGBROKER_PATCHER_CONFIG.getConfig(
            BOX_CONFIG_PATH
    );

    private static final Config CORRECT_BOX_RESOURCES_CONFIG = CORRECT_BOX_CONFIG.getConfig(
            RESOURCES_CONFIG_PATH
    );

    @Test
    public void parseConfigCorrectTest() {
        var actualBoxResourcesConfig = LogbrokerBoxResourcesConfigParser.parseConfig(
                CORRECT_BOX_RESOURCES_CONFIG
        );

        assertThatEquals(actualBoxResourcesConfig, EXPECTED_BOX_RESOURCES_CONFIG);
    }

    private static Stream<Config> parseConfigIncorrectTestParameters() {
        return Stream.of(
                CORRECT_BOX_RESOURCES_CONFIG.withoutPath(WHITE_LIST_CONFIG_PATH)
        );
    }

    @ParameterizedTest
    @MethodSource("parseConfigIncorrectTestParameters")
    public void parseConfigIncorrectTest(Config incorrectConfig) {
        assertThrows(
                RuntimeException.class,
                () -> LogbrokerBoxResourcesConfigParser.parseConfig(incorrectConfig)
        );
    }

    @Test
    public void parseResourcesConfigFromCorrectTest() {
        var expectedBoxResourcesConfig = LogbrokerBoxResourcesConfigParser.parseConfig(
                CORRECT_BOX_RESOURCES_CONFIG
        );

        var actualBoxResourcesConfig = LogbrokerBoxResourcesConfigParser.parseResourcesConfigFrom(
                CORRECT_BOX_CONFIG
        );

        assertThatEquals(actualBoxResourcesConfig, expectedBoxResourcesConfig);
    }

    @Test
    public void parseResourcesConfigFromMissingTest() {
        assertThrows(
                ConfigException.Missing.class,
                () -> LogbrokerBoxResourcesConfigParser.parseResourcesConfigFrom(
                        CORRECT_BOX_CONFIG.withoutPath(RESOURCES_CONFIG_PATH)
                )
        );
    }
}

class LogbrokerBoxResourcesConfigParserTestExpectedValues {
    private static final Map<String, AllComputeResources> EXPECTED_WHITE_LIST_RESOURCES = Map.of(
            "test_stage.test_du", new AllComputeResources(
                    1, 2,
                    3, 4 * PodSpecUtils.KILOBYTE,
                    5 * PodSpecUtils.MEGABYTE,
                    6 * PodSpecUtils.GIGABYTE,
                    7
            )
    );

    static final LogbrokerBoxResourcesConfig EXPECTED_BOX_RESOURCES_CONFIG = new LogbrokerBoxResourcesConfig(
            EXPECTED_WHITE_LIST_RESOURCES
    );
}

package ru.yandex.infra.stage.config;

import java.util.List;
import java.util.stream.Stream;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigValueFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.infra.stage.config.AllComputeResourcesConfigParser.ANONYMOUS_MEMORY_LIMIT_CONFIG_PATH;
import static ru.yandex.infra.stage.config.AllComputeResourcesConfigParser.HDD_CAPACITY_CONFIG_PATH;
import static ru.yandex.infra.stage.config.AllComputeResourcesConfigParser.MEMORY_GUARANTEE_CONFIG_PATH;
import static ru.yandex.infra.stage.config.AllComputeResourcesConfigParser.MEMORY_LIMIT_CONFIG_PATH;
import static ru.yandex.infra.stage.config.AllComputeResourcesConfigParser.THREAD_LIMIT_CONFIG_PATH;
import static ru.yandex.infra.stage.config.AllComputeResourcesConfigParser.VCPU_GUARANTEE_CONFIG_PATH;
import static ru.yandex.infra.stage.config.AllComputeResourcesConfigParser.VCPU_LIMIT_CONFIG_PATH;
import static ru.yandex.infra.stage.dto.AllComputeResourcesTest.DEFAULT_RESOURCES;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

public class AllComputeResourcesConfigParserTest {

    private static final Config DEFAULT_RESOURCES_CONFIG = AllComputeResourcesConfigParser.toConfig(
            DEFAULT_RESOURCES
    );

    private static final List<String> ALL_FIELD_CONFIG_PATHS = List.of(
            VCPU_GUARANTEE_CONFIG_PATH, VCPU_LIMIT_CONFIG_PATH,
            MEMORY_GUARANTEE_CONFIG_PATH, MEMORY_LIMIT_CONFIG_PATH,
            ANONYMOUS_MEMORY_LIMIT_CONFIG_PATH,
            HDD_CAPACITY_CONFIG_PATH,
            THREAD_LIMIT_CONFIG_PATH
    );

    @Test
    public void correctResourcesConfigTest() {
        var actualResources = AllComputeResourcesConfigParser.parseConfig(DEFAULT_RESOURCES_CONFIG);
        assertThatEquals(actualResources, DEFAULT_RESOURCES);
    }

    private static Stream<Config> missingFieldConfigTestParameters() {
        return ALL_FIELD_CONFIG_PATHS.stream()
                .map(DEFAULT_RESOURCES_CONFIG::withoutPath);
    }

    @ParameterizedTest
    @MethodSource("missingFieldConfigTestParameters")
    public void missingFieldConfigTest(Config missingFieldConfig) {
        incorrectConfigScenario(missingFieldConfig, ConfigException.Missing.class);
    }

    private static Stream<Config> incorrectFieldConfigTestParameters() {
        String incorrectValue = "incorrect_value";

        return ALL_FIELD_CONFIG_PATHS.stream()
                .map(path -> DEFAULT_RESOURCES_CONFIG.withValue(
                        path, ConfigValueFactory.fromAnyRef(incorrectValue)
                ));
    }

    @ParameterizedTest
    @MethodSource("incorrectFieldConfigTestParameters")
    public void incorrectFieldConfigTest(Config incorrectFieldConfig) {
        incorrectConfigScenario(incorrectFieldConfig, RuntimeException.class);
    }

    private static void incorrectConfigScenario(Config incorrectConfig,
                                                Class<? extends Throwable> expectedExceptionClass) {
        assertThrows(expectedExceptionClass, () -> AllComputeResourcesConfigParser.parseConfig(incorrectConfig));
    }
}

package ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.infra.stage.util.NamedArgument;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

public class DataRetentionGenerationConfigTest {

    static DataRetentionGenerationConfig createDataRetentionGenerationConfigMock(
            Optional<DataRetentionLimits> expectedLimits
    ) {
        var config = mock(DataRetentionGenerationConfig.class);

        when(config.getDataRetentionLimits())
                .thenReturn(expectedLimits);

        return config;
    }

    private static final String DEFAULT_AGE_LIMIT = "20d";
    private static final String DEFAULT_SIZE_LIMIT = "300mb";

    static final DataRetentionLimits DEFAULT_DATA_RETENTION_LIMITS = DataRetentionLimits.builder()
            .withAgeLimit(DEFAULT_AGE_LIMIT)
            .withSizeLimit(DEFAULT_SIZE_LIMIT)
            .build();

    private static final boolean DEFAULT_ENABLED = true;

    private static final DataRetentionGenerationConfig DEFAULT_DATA_RETENTION_CONFIG = new DataRetentionGenerationConfig(
            DEFAULT_ENABLED,
            DEFAULT_DATA_RETENTION_LIMITS
    );

    private static Stream<Arguments> getDataRetentionLimitsTestParameters() {
        return Stream.of(
                Arguments.of(
                        NamedArgument.of("enabled", true),
                        NamedArgument.of("default limits", Optional.of(DEFAULT_DATA_RETENTION_LIMITS))
                ),
                Arguments.of(
                        NamedArgument.of("disabled", false),
                        NamedArgument.of("empty limits", Optional.empty())
                )
        );
    }

    @ParameterizedTest
    @MethodSource("getDataRetentionLimitsTestParameters")
    void getDataRetentionLimitsTest(NamedArgument<Boolean> enabled,
                                    NamedArgument<Optional<DataRetentionLimits>> expectedLimits) {
        var dataRetentionConfig = DEFAULT_DATA_RETENTION_CONFIG.toBuilder()
                .withEnabled(enabled.getArgument())
                .build();

        var actualLimits = dataRetentionConfig.getDataRetentionLimits();

        assertThatEquals(actualLimits, expectedLimits.getArgument());
    }
}

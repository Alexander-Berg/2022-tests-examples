package ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.util.NamedArgument;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

public class VersionCalculatorImplTest {

    static VersionCalculator createVersionCalculatorMock(
            UnifiedAgentConfigVersion version
    ) {
        var versionCalculator = mock(VersionCalculator.class);

        when(versionCalculator.calculate(
                anyBoolean(),
                any(Optional.class)
        )).thenReturn(version);

        return versionCalculator;
    }

    private static final long DEFAULT_RELEASE_VERSION = TestData.DEFAULT_SIDECAR_REVISION;

    private static final UnifiedAgentConfigVersion DEFAULT_SMALL_VERSION = UnifiedAgentConfigVersion.V1;
    private static final long DEFAULT_SMALL_FIRST_AFFECTED_UNIFIED_AGENT_VERSION = DEFAULT_RELEASE_VERSION - 1;

    private static final UnifiedAgentConfigVersion DEFAULT_LARGE_VERSION = UnifiedAgentConfigVersion.V2;
    private static final long DEFAULT_LARGE_FIRST_AFFECTED_UNIFIED_AGENT_VERSION = DEFAULT_RELEASE_VERSION + 1;

    private static final Map<UnifiedAgentConfigVersion, Long> DEFAULT_VERSION_TO_FIRST_AFFECTED_UNIFIED_AGENT_VERSION = Map.of(
            DEFAULT_SMALL_VERSION, DEFAULT_SMALL_FIRST_AFFECTED_UNIFIED_AGENT_VERSION,
            DEFAULT_LARGE_VERSION, DEFAULT_LARGE_FIRST_AFFECTED_UNIFIED_AGENT_VERSION
    );

    private static final UnifiedAgentConfigVersion DEFAULT_DEFAULT_VERSION = DEFAULT_LARGE_VERSION;

    private static final VersionCalculator DEFAULT_VERSION_CALCULATOR = new VersionCalculatorImpl(
            DEFAULT_VERSION_TO_FIRST_AFFECTED_UNIFIED_AGENT_VERSION,
            DEFAULT_DEFAULT_VERSION
    );

    private static Stream<Arguments> calculateTestParameters() {
        var smallVersionArgument = NamedArgument.of(
                "small version", DEFAULT_SMALL_VERSION
        );

        var largeVersionArgument = NamedArgument.of(
                "large version", DEFAULT_LARGE_VERSION
        );

        var defaultVersionArgument = NamedArgument.of(
                "default version", DEFAULT_DEFAULT_VERSION
        );

        var streamBuilder = Stream.<Arguments>builder();

        var possibleErrorBoosterArguments = List.of(
                NamedArgument.of("eb disabled", false),
                NamedArgument.of("eb enabled", true)
        );

        possibleErrorBoosterArguments.forEach(errorBoosterArgument -> {
            var possibleArguments = List.of(
                    Arguments.of(
                            errorBoosterArgument,
                            NamedArgument.of(
                                    "empty release version",
                                    Optional.empty()
                            ),
                            defaultVersionArgument
                    ),
                    Arguments.of(
                            errorBoosterArgument,
                            NamedArgument.of(
                                    "release version smaller than small",
                                    Optional.of(DEFAULT_SMALL_FIRST_AFFECTED_UNIFIED_AGENT_VERSION - 1)
                            ),
                            defaultVersionArgument
                    ),
                    Arguments.of(
                            errorBoosterArgument,
                            NamedArgument.of(
                                    "release version equal to small",
                                    Optional.of(DEFAULT_SMALL_FIRST_AFFECTED_UNIFIED_AGENT_VERSION)
                            ),
                            smallVersionArgument
                    ),
                    Arguments.of(
                            errorBoosterArgument,
                            NamedArgument.of(
                                    "release version between small and large",
                                    Optional.of(DEFAULT_SMALL_FIRST_AFFECTED_UNIFIED_AGENT_VERSION + 1)
                            ),
                            smallVersionArgument
                    ),
                    Arguments.of(
                            errorBoosterArgument,
                            NamedArgument.of(
                                    "release version equal to large",
                                    Optional.of(DEFAULT_LARGE_FIRST_AFFECTED_UNIFIED_AGENT_VERSION)
                            ),
                            largeVersionArgument
                    ),
                    Arguments.of(
                            errorBoosterArgument,
                            NamedArgument.of(
                                    "release version more than large",
                                    Optional.of(DEFAULT_LARGE_FIRST_AFFECTED_UNIFIED_AGENT_VERSION + 1)
                            ),
                            largeVersionArgument
                    )
            );

            possibleArguments.forEach(
                    streamBuilder::add
            );
        });

        return streamBuilder.build();
    }

    @ParameterizedTest
    @MethodSource("calculateTestParameters")
    void calculateTest(NamedArgument<Boolean> errorBoosterEnabled,
                       NamedArgument<Optional<Long>> releaseVersionOptional,
                       NamedArgument<UnifiedAgentConfigVersion> expectedVersion) {

        var actualVersion = DEFAULT_VERSION_CALCULATOR.calculate(
                errorBoosterEnabled.getArgument(),
                releaseVersionOptional.getArgument()
        );

        assertThatEquals(actualVersion, expectedVersion.getArgument());
    }
}

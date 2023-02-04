package ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.dto.LogbrokerTopicRequest;
import ru.yandex.infra.stage.podspecs.SandboxResourceMeta;
import ru.yandex.infra.stage.util.NamedArgument;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.infra.stage.TestData.DEFAULT_SIDECAR_REVISION;
import static ru.yandex.infra.stage.deployunit.LogbrokerTopicConfigResolverTestCommunal.DEFAULT_COMMUNAL_TOPIC_REQUEST;
import static ru.yandex.infra.stage.deployunit.LogbrokerTopicConfigResolverTestCustom.DEFAULT_CUSTOM_TOPIC_REQUEST;
import static ru.yandex.infra.stage.podspecs.SandboxResourceMetaAttributesUtilsTest.createFlagCalculatorMock;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

public class ErrorBoosterIsEnabledPredicateImplTest {

    static ErrorBoosterIsEnabledPredicate createErrorBoosterIsEnabledPredicateMock(
            boolean errorBoosterEnabled
    ) {
        var errorBoosterIsEnabledPredicate = mock(ErrorBoosterIsEnabledPredicate.class);

        when(errorBoosterIsEnabledPredicate.test(
                any(Optional.class),
                any(Optional.class),
                any(LogbrokerTopicRequest.class)
        )).thenReturn(errorBoosterEnabled);

        return errorBoosterIsEnabledPredicate;
    }

    private static final long DEFAULT_FIRST_AFFECTED_UNIFIED_AGENT_VERSION = DEFAULT_SIDECAR_REVISION;
    private static final String DEFAULT_ENABLED_IN_RELEASE_FLAG_NAME = "enabled_in_release";

    private static final long DEFAULT_RELEASE_VERSION = DEFAULT_FIRST_AFFECTED_UNIFIED_AGENT_VERSION + 1;
    private static final SandboxResourceMeta DEFAULT_SANDBOX_META = TestData.RESOURCE_META;
    private static final boolean DEFAULT_RELEASE_FLAG_VALUE = true;
    private static final LogbrokerTopicRequest DEFAULT_TOPIC_REQUEST = DEFAULT_COMMUNAL_TOPIC_REQUEST;

    private static final ErrorBoosterIsEnabledPredicateImpl DEFAULT_ERROR_BOOSTER_GENERATION_CONFIG =
            new ErrorBoosterIsEnabledPredicateImpl(
                    DEFAULT_FIRST_AFFECTED_UNIFIED_AGENT_VERSION,
                    Optional.of(DEFAULT_ENABLED_IN_RELEASE_FLAG_NAME),
                    createFlagCalculatorMock(Map.of(DEFAULT_ENABLED_IN_RELEASE_FLAG_NAME, DEFAULT_RELEASE_FLAG_VALUE))
            );

    private static Stream<Arguments> enabledTestParameters() {
        return Stream.of(
                Arguments.of(
                        NamedArgument.of(
                                "not empty 'enabled by release' flag name",
                                Optional.of(DEFAULT_ENABLED_IN_RELEASE_FLAG_NAME)
                        ),
                        NamedArgument.of("enabled by release", true)
                ),
                Arguments.of(
                        NamedArgument.of(
                                "empty 'enabled by release' flag name",
                                Optional.empty()
                        ),
                        NamedArgument.of("not enabled by release", false)
                )
        );
    }

    @ParameterizedTest
    @MethodSource("enabledTestParameters")
    void enabledTest(
            NamedArgument<Optional<String>> enabledByReleaseFlagName,
            NamedArgument<Boolean> enabledByReleaseFlagValue) {
        var flagCalculator = createFlagCalculatorMock(Map.of(
                DEFAULT_ENABLED_IN_RELEASE_FLAG_NAME, enabledByReleaseFlagValue.getArgument()
        ));

        var actualEnabled = errorBoosterEnabledScenario(
                DEFAULT_ERROR_BOOSTER_GENERATION_CONFIG.toBuilder()
                    .withEnabledInReleaseFlagName(enabledByReleaseFlagName.getArgument())
                    .withFlagCalculator(flagCalculator)
        );

        assertThatEquals(actualEnabled, true);
    }

    private static Stream<NamedArgument<Optional<Long>>> disabledByReleaseVersionTestParameters() {
        return Stream.of(
                NamedArgument.of(
                        "lower than first affected version",
                        Optional.of(DEFAULT_FIRST_AFFECTED_UNIFIED_AGENT_VERSION - 1)
                ),
                NamedArgument.of(
                        Optional.empty()
                )
        );
    }

    @ParameterizedTest
    @MethodSource("disabledByReleaseVersionTestParameters")
    void disabledByReleaseVersionTest(NamedArgument<Optional<Long>> releaseVersion) {
        var actualEnabled = errorBoosterEnabledScenario(
                DEFAULT_ERROR_BOOSTER_GENERATION_CONFIG,
                releaseVersion.getArgument(),
                Optional.of(DEFAULT_SANDBOX_META),
                DEFAULT_TOPIC_REQUEST
        );

        assertThatEquals(actualEnabled, false);
    }

    private static Stream<Arguments> flagCalculatorNotUsedTestParameters() {
        return Stream.of(
                Arguments.of(
                        NamedArgument.of("empty 'enabled by release' flag name", Optional.empty())
                )
        );
    }

    @ParameterizedTest
    @MethodSource("flagCalculatorNotUsedTestParameters")
    void flagCalculatorNotUsedTest(NamedArgument<Optional<String>> enabledByReleaseFlagName) {
        var flagCalculator = createFlagCalculatorMock(Map.of());

        errorBoosterEnabledScenario(
                DEFAULT_ERROR_BOOSTER_GENERATION_CONFIG.toBuilder()
                        .withEnabledInReleaseFlagName(enabledByReleaseFlagName.getArgument())
                        .withFlagCalculator(flagCalculator)
        );

        verify(flagCalculator, never()).getFlagValue(
                any(Optional.class),
                anyString()
        );
    }

    @Test
    void flagCalculatorArgumentsTest() {
        var flagCalculator = createFlagCalculatorMock(Map.of(
                DEFAULT_ENABLED_IN_RELEASE_FLAG_NAME, DEFAULT_RELEASE_FLAG_VALUE
        ));

        errorBoosterEnabledScenario(
                DEFAULT_ERROR_BOOSTER_GENERATION_CONFIG.toBuilder()
                        .withFlagCalculator(flagCalculator)
        );

        verify(flagCalculator).getFlagValue(
                eq(Optional.of(DEFAULT_SANDBOX_META)),
                eq(DEFAULT_ENABLED_IN_RELEASE_FLAG_NAME)
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {false})
    void flagCalculatorUsedTest(boolean releaseFlagDisabledValue) {
        var flagCalculator = createFlagCalculatorMock(Map.of(
                DEFAULT_ENABLED_IN_RELEASE_FLAG_NAME, releaseFlagDisabledValue
        ));

        var actualEnabled = errorBoosterEnabledScenario(
                DEFAULT_ERROR_BOOSTER_GENERATION_CONFIG.toBuilder()
                        .withFlagCalculator(flagCalculator)
        );

        assertThatEquals(actualEnabled, false);
    }

    private static Stream<Arguments> disabledByTopicRequestTestParameters() {
        return Stream.of(
                Arguments.of(
                        NamedArgument.of("custom", DEFAULT_CUSTOM_TOPIC_REQUEST)
                )
        );
    }

    @ParameterizedTest
    @MethodSource("disabledByTopicRequestTestParameters")
    void disabledByTopicRequestTest(NamedArgument<LogbrokerTopicRequest> topicRequest) {
        var actualEnabled = errorBoosterEnabledScenario(
                DEFAULT_ERROR_BOOSTER_GENERATION_CONFIG,
                Optional.of(DEFAULT_RELEASE_VERSION),
                Optional.of(DEFAULT_SANDBOX_META),
                topicRequest.getArgument()
        );

        assertThatEquals(actualEnabled, false);
    }

    private static boolean errorBoosterEnabledScenario(ErrorBoosterIsEnabledPredicateImpl.Builder builder) {
        return errorBoosterEnabledScenario(
                builder.build(),
                Optional.of(DEFAULT_RELEASE_VERSION),
                Optional.of(DEFAULT_SANDBOX_META),
                DEFAULT_TOPIC_REQUEST
        );
    }

    private static boolean errorBoosterEnabledScenario(ErrorBoosterIsEnabledPredicate errorBoosterIsEnabledPredicate,
                                                       Optional<Long> releaseVersion,
                                                       Optional<SandboxResourceMeta> sandboxMeta,
                                                       LogbrokerTopicRequest topicRequest) {
        return errorBoosterIsEnabledPredicate.test(
                releaseVersion,
                sandboxMeta,
                topicRequest
        );
    }
}

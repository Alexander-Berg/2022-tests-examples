package ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.dto.LogbrokerCommunalTopicRequest;
import ru.yandex.infra.stage.dto.LogbrokerCustomTopicRequest;
import ru.yandex.infra.stage.dto.LogbrokerTopicRequest;
import ru.yandex.infra.stage.util.NamedArgument;

import static java.util.Collections.emptyMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.infra.stage.TestData.DEFAULT_UNIT_CONTEXT;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.ThrottlingGenerationConfig.UNLIMITED_LIMITS;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

public class ThrottlingGenerationConfigTest {

    static ThrottlingGenerationConfig createThrottlingGenerationConfigMock(ThrottlingLimits throttlingLimits) {
        var throttlingConfig = mock(ThrottlingGenerationConfig.class);

        when(throttlingConfig.getDeployUnitLimits(
                any(LogbrokerTopicRequest.class),
                any(Optional.class),
                anyString(),
                anyString()
        )).thenReturn(throttlingLimits);

        return throttlingConfig;
    }

    private static final String DEFAULT_CUSTOM_STAGE_RATE_LIMIT = "2tb";
    private static final int DEFAULT_CUSTOM_STAGE_MESSAGES_RATE_LIMIT = 123456;
    static final ThrottlingLimits DEFAULT_THROTTLING_CUSTOM_LIMITS = new ThrottlingLimits(
            DEFAULT_CUSTOM_STAGE_RATE_LIMIT, DEFAULT_CUSTOM_STAGE_MESSAGES_RATE_LIMIT
    );

    private static final long DEFAULT_FIRST_AFFECTED_UNIFIED_AGENT_VERSION = TestData.DEFAULT_SIDECAR_REVISION;
    static final String DEFAULT_PATCHER_LIMITS_KEY = "patcher_limits_for_test";

    private static final ThrottlingGenerationConfig DEFAULT_THROTTLING_CONFIG = new ThrottlingGenerationConfig(
            emptyMap(),
            DEFAULT_FIRST_AFFECTED_UNIFIED_AGENT_VERSION,
            Map.of(
                    DEFAULT_PATCHER_LIMITS_KEY, DEFAULT_THROTTLING_CUSTOM_LIMITS
            )
    );

    private static Stream<Arguments> getDeployUnitLimitsTestParameters() {
        var defaultLimits = UNLIMITED_LIMITS;

        var defaultLimitsArgument = NamedArgument.of(
                "default limits", defaultLimits
        );

        var customDeployUnitLimits = new ThrottlingLimits(
                "1" + defaultLimits.getMaxRate(),
                2 * defaultLimits.getMaxMessagesRate()
        );

        var customDeployUnitLimitsArgument = NamedArgument.of(
                "custom deploy unit limits",
                customDeployUnitLimits
        );

        var customizedDeployUnitsLimits = NamedArgument.of(
                "customized deploy unit limits",
                Map.of(DEFAULT_UNIT_CONTEXT.getFullDeployUnitId(), customDeployUnitLimits)
        );

        var emptyDeployUnitLimits = NamedArgument.of(
                "empty deploy unit limits", emptyMap()
        );

        long firstAffectedUnifiedAgentVersion = DEFAULT_FIRST_AFFECTED_UNIFIED_AGENT_VERSION;

        var affectedUnifiedAgentVersionArgument = NamedArgument.of(
                "affected unified agent version", Optional.of(firstAffectedUnifiedAgentVersion)
        );

        var notAffectedUnifiedAgentVersionArguments = List.of(
                NamedArgument.of(
                        "empty unified agent version", Optional.<Long>empty()
                ),
                NamedArgument.of(
                        "not affected unified agent version", Optional.of(firstAffectedUnifiedAgentVersion - 1)
                )
        );

        var communalTopic = NamedArgument.of(
                "communal topic",
                LogbrokerCommunalTopicRequest.INSTANCE
        );

        var customTopic = NamedArgument.of(
                "custom topic",
                new LogbrokerCustomTopicRequest(
                    TestData.LOGBROKER_TOPIC_DESCRIPTION,
                    TestData.SECRET_SELECTOR
                )
        );

        var emptyPatcherLimitsArgument= NamedArgument.of(
                "empty patcher limits", emptyMap()
        );

        var patcherLimits = new ThrottlingLimits(
                "2" + defaultLimits.getMaxRate(),
                3 *  defaultLimits.getMaxMessagesRate()
        );

        var presentedPatcherLimitsArgument = NamedArgument.of(
                "presented patcher limits", Map.of(DEFAULT_PATCHER_LIMITS_KEY, patcherLimits)
        );

        var patcherLimitsArgument = NamedArgument.of(
                "patcher limits", patcherLimits
        );

        var argumentStreamBuilder = Stream.<Arguments>builder();

        // if custom topic - ALWAYS default limits
        argumentStreamBuilder.add(Arguments.of(
                customTopic,
                affectedUnifiedAgentVersionArgument,
                customizedDeployUnitsLimits,
                presentedPatcherLimitsArgument,
                defaultLimitsArgument
        ));

        // if unified agent version is old - always default limits
        notAffectedUnifiedAgentVersionArguments.forEach(notAffectedUnifiedAgentVersionArgument ->
                argumentStreamBuilder.add(Arguments.of(
                        communalTopic,
                        notAffectedUnifiedAgentVersionArgument,
                        customizedDeployUnitsLimits,
                        presentedPatcherLimitsArgument,
                        defaultLimitsArgument
                ))
        );

        // if stage in white list - always custom limits
        argumentStreamBuilder.add(Arguments.of(
                communalTopic,
                affectedUnifiedAgentVersionArgument, // empty -> default
                customizedDeployUnitsLimits,
                presentedPatcherLimitsArgument,
                customDeployUnitLimitsArgument
        ));

        // if limits for patcher are presented - returns them
        argumentStreamBuilder.add(Arguments.of(
                communalTopic,
                affectedUnifiedAgentVersionArgument,
                emptyDeployUnitLimits,
                presentedPatcherLimitsArgument,
                patcherLimitsArgument
        ));

        // else - default limits
        argumentStreamBuilder.add(Arguments.of(
                communalTopic,
                affectedUnifiedAgentVersionArgument,
                emptyDeployUnitLimits,
                emptyPatcherLimitsArgument,
                defaultLimitsArgument
        ));

        return argumentStreamBuilder.build();
    }

    @ParameterizedTest
    @MethodSource("getDeployUnitLimitsTestParameters")
    void getDeployUnitLimitsTest(NamedArgument<LogbrokerTopicRequest> topicRequest,
                                 NamedArgument<Optional<Long>> unifiedAgentVersion,
                                 NamedArgument<Map<String, ThrottlingLimits>> deployUnitLimits,
                                 NamedArgument<Map<String, ThrottlingLimits>> patcherLimits,
                                 NamedArgument<ThrottlingLimits> expectedLimits) {
        var throttlingConfig = DEFAULT_THROTTLING_CONFIG.toBuilder()
                .withDeployUnitLimits(deployUnitLimits.getArgument())
                .withPatcherLimits(patcherLimits.getArgument())
                .build();

        var actualLimits = throttlingConfig.getDeployUnitLimits(
                topicRequest.getArgument(),
                unifiedAgentVersion.getArgument(),
                DEFAULT_UNIT_CONTEXT.getFullDeployUnitId(),
                DEFAULT_PATCHER_LIMITS_KEY
        );

        assertThatEquals(actualLimits, expectedLimits.getArgument());
    }
}

package ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.dto.LogbrokerTopicDescription;
import ru.yandex.infra.stage.util.NamedArgument;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.DataRetentionGenerationConfigTest.DEFAULT_DATA_RETENTION_LIMITS;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.ThrottlingGenerationConfigTest.DEFAULT_THROTTLING_CUSTOM_LIMITS;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.UnifiedAgentConfigTest.DEFAULT_CLUSTERS;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.UnifiedAgentConfigTest.DEFAULT_DEPLOY_UNIT_ID;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.UnifiedAgentConfigTest.DEFAULT_STAGE_ID;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.UnifiedAgentConfigTest.DEFAULT_STATIC_SECRET;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.UnifiedAgentConfigTest.DEFAULT_TOPIC_DESCRIPTION;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

public class VersionFactoryImplTest {

    static VersionFactory createVersionFactoryMock(UnifiedAgentConfig unifiedAgentConfig) {
        var versionFactory = mock(VersionFactory.class);

        when(versionFactory.build(
                any(UnifiedAgentConfigVersion.class),
                anyLong(),
                anyString(),
                anyString(),
                anyString(),
                any(ThrottlingPolicy.class),
                any(ThrottlingLimits.class),
                any(Optional.class),
                anySet(),
                any(LogbrokerTopicDescription.class)
        )).thenReturn(unifiedAgentConfig);

        return versionFactory;
    }

    static final long DEFAULT_BUILD_TASK_ID = TestData.TASK_ID;

    private static final ThrottlingPolicy DEFAULT_THROTTLING_POLICY = ThrottlingPolicy.THROTTLING_BEFORE_COMPRESSION;

    private static final UnifiedAgentConfigV1 DEFAULT_UNIFIED_AGENT_CONFIG_V1 =
            UnifiedAgentConfigTest.DEFAULT_UNIFIED_AGENT_CONFIG_V1
                    .withBuildTaskId(DEFAULT_BUILD_TASK_ID);

    private static final UnifiedAgentConfigV2 DEFAULT_UNIFIED_AGENT_CONFIG_V2 =
            UnifiedAgentConfigTest.DEFAULT_UNIFIED_AGENT_CONFIG_V2.toBuilder()
                    .withBuildTaskId(DEFAULT_BUILD_TASK_ID)
                    .build();

    private static Stream<Arguments> buildTestParameters() {
        return Stream.of(
                Arguments.of(
                        NamedArgument.of(UnifiedAgentConfigVersion.V1),
                        NamedArgument.of("v1", DEFAULT_UNIFIED_AGENT_CONFIG_V1)
                ),
                Arguments.of(
                        NamedArgument.of(UnifiedAgentConfigVersion.V2),
                        NamedArgument.of("v2", DEFAULT_UNIFIED_AGENT_CONFIG_V2)
                )
        );
    }

    @ParameterizedTest
    @MethodSource("buildTestParameters")
    void buildTest(NamedArgument<UnifiedAgentConfigVersion> version,
                   NamedArgument<UnifiedAgentConfig> expectedConfig) {
        var actualConfig = VersionFactoryImpl.INSTANCE.build(
                version.getArgument(),
                DEFAULT_BUILD_TASK_ID,
                DEFAULT_STATIC_SECRET,
                DEFAULT_STAGE_ID,
                DEFAULT_DEPLOY_UNIT_ID,
                DEFAULT_THROTTLING_POLICY,
                DEFAULT_THROTTLING_CUSTOM_LIMITS,
                Optional.of(DEFAULT_DATA_RETENTION_LIMITS),
                DEFAULT_CLUSTERS,
                DEFAULT_TOPIC_DESCRIPTION
        );

        assertThatEquals(actualConfig, expectedConfig.getArgument());
    }
}

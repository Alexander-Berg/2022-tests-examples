package ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import one.util.streamex.EntryStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.infra.controller.util.ResourceUtils;
import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.dto.LogbrokerTopicDescription;
import ru.yandex.infra.stage.util.AssertUtils;

import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.DataRetentionGenerationConfigTest.DEFAULT_DATA_RETENTION_LIMITS;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.ThrottlingGenerationConfigTest.DEFAULT_THROTTLING_CUSTOM_LIMITS;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.ThrottlingPolicy.DISABLED;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.ThrottlingPolicy.THROTTLING_AFTER_COMPRESSION;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.ThrottlingPolicy.THROTTLING_BEFORE_COMPRESSION;
import static ru.yandex.infra.stage.util.JsonUtils.yamlToJson;

public class UnifiedAgentConfigTest {

    private static final String UNIFIED_AGENT_CONFIG_TEST_DIRECTORY = "logbroker/unified_agent/";

    private static final long DEFAULT_BUILD_TASK_ID = 678106182;
    private static final long DEFAULT_BUILD_TASK_ID_ERROR_BOOSTER = 1005299706;

    static final String DEFAULT_STAGE_ID = TestData.DEFAULT_STAGE_ID;
    static final String DEFAULT_DEPLOY_UNIT_ID = TestData.DEPLOY_UNIT_ID;

    static final Set<String> DEFAULT_CLUSTERS = ImmutableSet.of("sas-test");
    static final String DEFAULT_STATIC_SECRET = "TEST_STATIC_SECRET";

    static final ThrottlingPolicy DEFAULT_THROTTLING_POLICY = THROTTLING_BEFORE_COMPRESSION;

    static final LogbrokerTopicDescription DEFAULT_TOPIC_DESCRIPTION = TestData.LOGBROKER_TOPIC_DESCRIPTION;

    static final UnifiedAgentConfigV1 DEFAULT_UNIFIED_AGENT_CONFIG_V1 = new UnifiedAgentConfigV1(
            DEFAULT_BUILD_TASK_ID,
            DEFAULT_STATIC_SECRET,
            DEFAULT_STAGE_ID,
            DEFAULT_DEPLOY_UNIT_ID,
            DEFAULT_THROTTLING_POLICY,
            DEFAULT_THROTTLING_CUSTOM_LIMITS,
            DEFAULT_CLUSTERS,
            DEFAULT_TOPIC_DESCRIPTION
    );

    static final UnifiedAgentConfigV2 DEFAULT_UNIFIED_AGENT_CONFIG_V2 = new UnifiedAgentConfigV2(
            DEFAULT_BUILD_TASK_ID_ERROR_BOOSTER,
            DEFAULT_STATIC_SECRET,
            DEFAULT_STAGE_ID,
            DEFAULT_DEPLOY_UNIT_ID,
            DEFAULT_THROTTLING_CUSTOM_LIMITS,
            Optional.of(DEFAULT_DATA_RETENTION_LIMITS),
            DEFAULT_CLUSTERS,
            DEFAULT_TOPIC_DESCRIPTION
    );

    private static Stream<Arguments> unifiedAgentConfigV1TestParameters() {
        Map<ThrottlingPolicy, String> throttlingPolicyToExpectedConfigFileName = ImmutableMap.of(
                DISABLED, "unified_agent.yaml",
                THROTTLING_AFTER_COMPRESSION, "unified_agent_with_yd_throttling_after_compression.yaml",
                THROTTLING_BEFORE_COMPRESSION, "unified_agent_with_yd_throttling_before_compression.yaml"
        );

        var streamBuilder = Stream.<Arguments>builder();

        EntryStream.of(throttlingPolicyToExpectedConfigFileName)
                .mapKeys(DEFAULT_UNIFIED_AGENT_CONFIG_V1::withThrottlingPolicy)
                .map(e -> Arguments.of(e.getKey(), e.getValue()))
                .forEach(streamBuilder::add);

        return streamBuilder.build();
    }

    @ParameterizedTest
    @MethodSource("unifiedAgentConfigV1TestParameters")
    void unifiedAgentConfigV1Test(UnifiedAgentConfig unifiedAgentConfig, String expectedFileName) {
        unifiedAgentConfigScenario(unifiedAgentConfig.toJsonString(), expectedFileName);
    }

    private static Stream<Arguments> unifiedAgentConfigV2TestParameters() {
        return Stream.of(
                Arguments.of(DEFAULT_UNIFIED_AGENT_CONFIG_V2, "unified_agent_v2.yaml")
        );
    }

    @ParameterizedTest
    @MethodSource("unifiedAgentConfigV2TestParameters")
    void unifiedAgentConfigV2Test(UnifiedAgentConfig unifiedAgentConfig,
                                  String expectedFileName) {
        unifiedAgentConfigScenario(unifiedAgentConfig.toJsonString(), expectedFileName);
    }

    private static Stream<Arguments> unifiedAgentConfigV2DataRetentionTestParameters() {
        Map<Optional<DataRetentionLimits>, String> dataRetentionLimitsToExpectedConfigFileNameSuffix = ImmutableMap.of(
                Optional.empty(), "empty",
                Optional.of(DEFAULT_DATA_RETENTION_LIMITS.toBuilder().withoutAgeLimit().build()), "age_empty",
                Optional.of(DEFAULT_DATA_RETENTION_LIMITS.toBuilder().withoutSizeLimit().build()), "size_empty"
        );

        var streamBuilder = Stream.<Arguments>builder();

        EntryStream.of(dataRetentionLimitsToExpectedConfigFileNameSuffix)
                .mapKeys(dataRetentionLimitsOptional ->
                        DEFAULT_UNIFIED_AGENT_CONFIG_V2.toBuilder()
                                .withDataRetentionLimitsOptional(dataRetentionLimitsOptional)
                                .build()
                )
                .mapValues(suffix -> String.format(
                        "data_retention/unified_agent_v2_data_retention_%s.yaml", suffix
                ))
                .map(e -> Arguments.of(e.getKey(), e.getValue()))
                .forEach(streamBuilder::add);

        return streamBuilder.build();
    }

    @ParameterizedTest
    @MethodSource("unifiedAgentConfigV2DataRetentionTestParameters")
    void unifiedAgentConfigV2DataRetentionTest(UnifiedAgentConfig unifiedAgentConfig,
                                               String expectedFileName) {
        unifiedAgentConfigScenario(unifiedAgentConfig.toJsonString(), expectedFileName);
    }

    private static void unifiedAgentConfigScenario(String actualJsonString, String expectedFileName) {
        String expectedJson = yamlToJson(
                ResourceUtils.readResource(UNIFIED_AGENT_CONFIG_TEST_DIRECTORY + expectedFileName)
        );

        AssertUtils.assertJsonStringEquals(actualJsonString, expectedJson);
    }
}

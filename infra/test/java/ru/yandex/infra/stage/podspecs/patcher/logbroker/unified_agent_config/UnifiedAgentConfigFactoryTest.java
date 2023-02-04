package ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.dto.LogbrokerTopicConfig;
import ru.yandex.infra.stage.dto.LogbrokerTopicDescription;
import ru.yandex.infra.stage.dto.LogbrokerTopicRequest;
import ru.yandex.infra.stage.dto.SandboxResourceInfo;
import ru.yandex.infra.stage.podspecs.PodSpecUtils;
import ru.yandex.infra.stage.podspecs.ResourceSupplier;
import ru.yandex.infra.stage.podspecs.ResourceWithMeta;
import ru.yandex.infra.stage.podspecs.SandboxResourceMeta;
import ru.yandex.infra.stage.util.NamedArgument;
import ru.yandex.yp.client.pods.TLayer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.infra.stage.TestData.DEFAULT_LOGBROKER_TOPIC_CONFIG;
import static ru.yandex.infra.stage.TestData.DEFAULT_LOGBROKER_TOPIC_REQUEST;
import static ru.yandex.infra.stage.TestData.DEFAULT_UNIT_CONTEXT;
import static ru.yandex.infra.stage.podspecs.PodSpecUtilsTest.createResourceSupplierMock;
import static ru.yandex.infra.stage.podspecs.PodSpecUtilsTest.createSandboxResourceIdCalculatorMock;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.BuildTaskIdCalculatorImplTest.DEFAULT_LOGBROKER_LAYER;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.BuildTaskIdCalculatorImplTest.createBuildTaskIdCalculatorMock;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.DataRetentionGenerationConfigTest.DEFAULT_DATA_RETENTION_LIMITS;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.DataRetentionGenerationConfigTest.createDataRetentionGenerationConfigMock;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.ErrorBoosterIsEnabledPredicateImplTest.createErrorBoosterIsEnabledPredicateMock;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.ThrottlingGenerationConfigTest.DEFAULT_PATCHER_LIMITS_KEY;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.ThrottlingGenerationConfigTest.DEFAULT_THROTTLING_CUSTOM_LIMITS;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.ThrottlingGenerationConfigTest.createThrottlingGenerationConfigMock;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.ThrottlingPolicyCalculatorImplTest.createThrottlingPolicyCalculatorMock;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.UnifiedAgentConfigTest.DEFAULT_CLUSTERS;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.UnifiedAgentConfigTest.DEFAULT_DEPLOY_UNIT_ID;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.UnifiedAgentConfigTest.DEFAULT_STAGE_ID;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.UnifiedAgentConfigTest.DEFAULT_STATIC_SECRET;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.UnifiedAgentConfigTest.DEFAULT_THROTTLING_POLICY;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.UnifiedAgentConfigTest.DEFAULT_TOPIC_DESCRIPTION;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.VersionCalculatorImplTest.createVersionCalculatorMock;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.VersionFactoryImplTest.DEFAULT_BUILD_TASK_ID;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.VersionFactoryImplTest.createVersionFactoryMock;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;
import static ru.yandex.infra.stage.util.JsonUtils.toJson;

public class UnifiedAgentConfigFactoryTest {

    private static final long DEFAULT_RELEASE_VERSION = TestData.DEFAULT_SIDECAR_REVISION;
    private static final SandboxResourceMeta DEFAULT_UNIFIED_AGENT_SANDBOX_META = TestData.RESOURCE_META;

    private static final SandboxResourceInfo DEFAULT_LOGBROKER_TOOLS_SIDECAR_INFO = TestData.DEFAULT_SIDECAR_RESOURCE_INFO;

    private static final SandboxResourceMeta DEFAULT_RESOURCE_META = TestData.RESOURCE_META;

    private static final ResourceSupplier DEFAULT_RESOURCE_SUPPLIER = createResourceSupplierMock(
            new ResourceWithMeta(TestData.DOWNLOADABLE_RESOURCE, Optional.of(TestData.RESOURCE_META))
    );

    private static final String DEFAULT_FULL_DEPLOY_UNIT_ID = DEFAULT_UNIT_CONTEXT.getFullDeployUnitId();

    private static final Collection<TLayer> DEFAULT_LAYERS = List.of(DEFAULT_LOGBROKER_LAYER);

    private static final String DEFAULT_UNIFIED_AGENT_CONFIG_JSON = toJson(Map.of("ua key", "ua value"));
    public static final UnifiedAgentConfig DEFAULT_UNIFIED_AGENT_CONFIG = () -> DEFAULT_UNIFIED_AGENT_CONFIG_JSON;

    private static final boolean DEFAULT_ERROR_BOOSTER_ENABLED = true;
    private static final UnifiedAgentConfigVersion DEFAULT_CONFIG_VERSION = UnifiedAgentConfigVersion.V2;

    private static final UnifiedAgentConfigFactory DEFAULT_UNIFIED_AGENT_CONFIG_FACTORY = new UnifiedAgentConfigFactory(
            createThrottlingGenerationConfigMock(DEFAULT_THROTTLING_CUSTOM_LIMITS),
            createErrorBoosterIsEnabledPredicateMock(DEFAULT_ERROR_BOOSTER_ENABLED),
            createDataRetentionGenerationConfigMock(Optional.of(DEFAULT_DATA_RETENTION_LIMITS)),
            (podAgentPayloadLayers, unifiedAgentSandboxMeta) -> DEFAULT_BUILD_TASK_ID,
            (resourceInfo, defaultResourceSupplier) -> Optional.of(DEFAULT_RELEASE_VERSION),
            unifiedAgentSandboxMeta -> DEFAULT_THROTTLING_POLICY,
            createVersionCalculatorMock(DEFAULT_CONFIG_VERSION),
            createVersionFactoryMock(DEFAULT_UNIFIED_AGENT_CONFIG)
    );

    @Test
    void releaseVersionCalculatorArgumentsTest() {
        var releaseVersionCalculator = createSandboxResourceIdCalculatorMock(
                Optional.of(DEFAULT_RELEASE_VERSION)
        );

        unifiedAgentConfigBuildScenario(
                DEFAULT_UNIFIED_AGENT_CONFIG_FACTORY.toBuilder()
                        .withReleaseVersionCalculator(releaseVersionCalculator)
        );

        verify(releaseVersionCalculator).calculate(
                eq(Optional.of(DEFAULT_LOGBROKER_TOOLS_SIDECAR_INFO)),
                eq(DEFAULT_RESOURCE_SUPPLIER)
        );
    }

    @ParameterizedTest
    @ValueSource(longs = {DEFAULT_RELEASE_VERSION, DEFAULT_RELEASE_VERSION + 1})
    @NullSource
    void releaseVersionCalculatorUsedTest(Long expectedReleaseVersionNullable) {
        var expectedReleaseVersion = Optional.ofNullable(expectedReleaseVersionNullable);

        PodSpecUtils.SandboxResourceIdCalculator releaseVersionCalculator =
                (resourceInfo, defaultResourceSupplier) -> expectedReleaseVersion;

        var throttlingConfig = createThrottlingGenerationConfigMock(
                DEFAULT_THROTTLING_CUSTOM_LIMITS
        );

        var errorBoosterIsEnabledPredicate = createErrorBoosterIsEnabledPredicateMock(
                DEFAULT_ERROR_BOOSTER_ENABLED
        );

        var versionCalculator = createVersionCalculatorMock(
                DEFAULT_CONFIG_VERSION
        );

        unifiedAgentConfigBuildScenario(
                DEFAULT_UNIFIED_AGENT_CONFIG_FACTORY.toBuilder()
                        .withReleaseVersionCalculator(releaseVersionCalculator)
                        .withThrottlingGenerationConfig(throttlingConfig)
                        .withErrorBoosterIsEnabledPredicate(errorBoosterIsEnabledPredicate)
                        .withVersionCalculator(versionCalculator)
        );

        verify(throttlingConfig).getDeployUnitLimits(
                any(LogbrokerTopicRequest.class),
                eq(expectedReleaseVersion),
                anyString(),
                anyString()
        );

        verify(errorBoosterIsEnabledPredicate).test(
                eq(expectedReleaseVersion),
                any(Optional.class),
                any(LogbrokerTopicRequest.class)
        );

        verify(versionCalculator).calculate(
                anyBoolean(),
                eq(expectedReleaseVersion)
        );
    }

    @Test
    void throttlingGenerationConfigArgumentsTest() {
        var throttlingConfig = createThrottlingGenerationConfigMock(
                DEFAULT_THROTTLING_CUSTOM_LIMITS
        );

        unifiedAgentConfigBuildScenario(
                DEFAULT_UNIFIED_AGENT_CONFIG_FACTORY.toBuilder()
                        .withThrottlingGenerationConfig(throttlingConfig)
        );

        verify(throttlingConfig).getDeployUnitLimits(
                eq(DEFAULT_LOGBROKER_TOPIC_REQUEST),
                eq(Optional.of(DEFAULT_RELEASE_VERSION)),
                eq(DEFAULT_FULL_DEPLOY_UNIT_ID),
                eq(DEFAULT_PATCHER_LIMITS_KEY)
        );
    }

    private static Stream<ThrottlingLimits> throttlingGenerationConfigUsedTestParameters() {
        return Stream.of(
                DEFAULT_THROTTLING_CUSTOM_LIMITS,
                new ThrottlingLimits(
                        "1" + DEFAULT_THROTTLING_CUSTOM_LIMITS.getMaxRate(),
                        2 * DEFAULT_THROTTLING_CUSTOM_LIMITS.getMaxMessagesRate()
                )
        );
    }

    @ParameterizedTest
    @MethodSource("throttlingGenerationConfigUsedTestParameters")
    void throttlingGenerationConfigUsedTest(ThrottlingLimits expectedThrottlingLimits) {
        var throttlingConfig = createThrottlingGenerationConfigMock(expectedThrottlingLimits);

        var versionFactory = createVersionFactoryMock(
                DEFAULT_UNIFIED_AGENT_CONFIG
        );

        unifiedAgentConfigBuildScenario(
                DEFAULT_UNIFIED_AGENT_CONFIG_FACTORY.toBuilder()
                        .withThrottlingGenerationConfig(throttlingConfig)
                        .withVersionFactory(versionFactory)
        );

        verify(versionFactory).build(
                any(UnifiedAgentConfigVersion.class),
                anyLong(),
                anyString(),
                anyString(),
                anyString(),
                any(ThrottlingPolicy.class),
                eq(expectedThrottlingLimits),
                any(Optional.class),
                anySet(),
                any(LogbrokerTopicDescription.class)
        );
    }

    @Test
    void dataRetentionGenerationConfigArgumentsTest() {
        var dataRetentionGenerationConfig = createDataRetentionGenerationConfigMock(
                Optional.of(DEFAULT_DATA_RETENTION_LIMITS)
        );

        unifiedAgentConfigBuildScenario(
                DEFAULT_UNIFIED_AGENT_CONFIG_FACTORY.toBuilder()
                        .withDataRetentionGenerationConfig(dataRetentionGenerationConfig)
        );

        verify(dataRetentionGenerationConfig).getDataRetentionLimits();
    }

    private static Stream<Optional<DataRetentionLimits>> dataRetentionGenerationConfigUsedTestParameters() {
        return Stream.of(
                Optional.of(DEFAULT_DATA_RETENTION_LIMITS),
                Optional.empty()
        );
    }

    @ParameterizedTest
    @MethodSource("dataRetentionGenerationConfigUsedTestParameters")
    void dataRetentionGenerationConfigUsedTest(Optional<DataRetentionLimits> expectedDataRetentionLimits) {
        var dataRetentionGenerationConfig = createDataRetentionGenerationConfigMock(
                expectedDataRetentionLimits
        );

        var versionFactory = createVersionFactoryMock(
                DEFAULT_UNIFIED_AGENT_CONFIG
        );

        unifiedAgentConfigBuildScenario(
                DEFAULT_UNIFIED_AGENT_CONFIG_FACTORY.toBuilder()
                        .withDataRetentionGenerationConfig(dataRetentionGenerationConfig)
                        .withVersionFactory(versionFactory)
        );

        verify(versionFactory).build(
                any(UnifiedAgentConfigVersion.class),
                anyLong(),
                anyString(),
                anyString(),
                anyString(),
                any(ThrottlingPolicy.class),
                any(ThrottlingLimits.class),
                eq(expectedDataRetentionLimits),
                anySet(),
                any(LogbrokerTopicDescription.class)
        );
    }

    @Test
    void buildTaskIdCalculatorArgumentsTest() {
        var buildTaskIdCalculator = createBuildTaskIdCalculatorMock(
                DEFAULT_BUILD_TASK_ID
        );

        unifiedAgentConfigBuildScenario(
                DEFAULT_UNIFIED_AGENT_CONFIG_FACTORY.toBuilder()
                        .withBuildTaskIdCalculator(buildTaskIdCalculator)
        );

        verify(buildTaskIdCalculator).calculate(
                eq(DEFAULT_LAYERS),
                eq(Optional.of(DEFAULT_RESOURCE_META))
        );
    }

    @ParameterizedTest
    @ValueSource(longs = { DEFAULT_BUILD_TASK_ID, DEFAULT_BUILD_TASK_ID + 1 })
    void buildTaskIdCalculatorUsedTest(long expectedBuildTaskId) {
        var buildTaskIdCalculator = createBuildTaskIdCalculatorMock(expectedBuildTaskId);

        var versionFactory = createVersionFactoryMock(
                DEFAULT_UNIFIED_AGENT_CONFIG
        );

        unifiedAgentConfigBuildScenario(
                DEFAULT_UNIFIED_AGENT_CONFIG_FACTORY.toBuilder()
                        .withBuildTaskIdCalculator(buildTaskIdCalculator)
                        .withVersionFactory(versionFactory)
        );

        verify(versionFactory).build(
                any(UnifiedAgentConfigVersion.class),
                eq(expectedBuildTaskId),
                anyString(),
                anyString(),
                anyString(),
                any(ThrottlingPolicy.class),
                any(ThrottlingLimits.class),
                any(Optional.class),
                anySet(),
                any(LogbrokerTopicDescription.class)
        );
    }

    @Test
    void throttlingPolicyCalculatorArgumentsTest() {
        var throttlingPolicyCalculator = createThrottlingPolicyCalculatorMock(
                DEFAULT_THROTTLING_POLICY
        );

        unifiedAgentConfigBuildScenario(
                DEFAULT_UNIFIED_AGENT_CONFIG_FACTORY.toBuilder()
                        .withThrottlingPolicyCalculator(throttlingPolicyCalculator)
        );

        verify(throttlingPolicyCalculator).calculate(
                eq(Optional.of(DEFAULT_UNIFIED_AGENT_SANDBOX_META))
        );
    }

    @ParameterizedTest
    @EnumSource(ThrottlingPolicy.class)
    void throttlingPolicyCalculatorUsedTest(ThrottlingPolicy expectedThrottlingPolicy) {
        var throttlingPolicyCalculator = createThrottlingPolicyCalculatorMock(
                expectedThrottlingPolicy
        );

        var versionFactory = createVersionFactoryMock(
                DEFAULT_UNIFIED_AGENT_CONFIG
        );

        unifiedAgentConfigBuildScenario(
                DEFAULT_UNIFIED_AGENT_CONFIG_FACTORY.toBuilder()
                        .withThrottlingPolicyCalculator(throttlingPolicyCalculator)
                        .withVersionFactory(versionFactory)
        );

        verify(versionFactory).build(
                any(UnifiedAgentConfigVersion.class),
                anyLong(),
                anyString(),
                anyString(),
                anyString(),
                eq(expectedThrottlingPolicy),
                any(ThrottlingLimits.class),
                any(Optional.class),
                anySet(),
                any(LogbrokerTopicDescription.class)
        );
    }

    @Test
    void errorBoosterIsEnabledPredicateArgumentsTest() {
        var errorBoosterIsEnabledPredicate = createErrorBoosterIsEnabledPredicateMock(
                DEFAULT_ERROR_BOOSTER_ENABLED
        );

        unifiedAgentConfigBuildScenario(
                DEFAULT_UNIFIED_AGENT_CONFIG_FACTORY.toBuilder()
                        .withErrorBoosterIsEnabledPredicate(errorBoosterIsEnabledPredicate)
        );

        verify(errorBoosterIsEnabledPredicate).test(
                eq(Optional.of(DEFAULT_RELEASE_VERSION)),
                eq(Optional.of(DEFAULT_UNIFIED_AGENT_SANDBOX_META)),
                eq(DEFAULT_LOGBROKER_TOPIC_REQUEST)
        );
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void errorBoosterIsEnabledPredicateUsedTest(boolean expectedErrorBoosterEnabled) {
        var errorBoosterIsEnabledPredicate = createErrorBoosterIsEnabledPredicateMock(
                expectedErrorBoosterEnabled
        );

        var versionCalculator = createVersionCalculatorMock(
                DEFAULT_CONFIG_VERSION
        );

        unifiedAgentConfigBuildScenario(
                DEFAULT_UNIFIED_AGENT_CONFIG_FACTORY.toBuilder()
                        .withErrorBoosterIsEnabledPredicate(errorBoosterIsEnabledPredicate)
                        .withVersionCalculator(versionCalculator)
        );

        verify(versionCalculator).calculate(
                eq(expectedErrorBoosterEnabled),
                any(Optional.class)
        );
    }

    @Test
    void versionCalculatorArgumentsTest() {
        var versionCalculator = createVersionCalculatorMock(
                DEFAULT_CONFIG_VERSION
        );

        unifiedAgentConfigBuildScenario(
                DEFAULT_UNIFIED_AGENT_CONFIG_FACTORY.toBuilder()
                        .withVersionCalculator(versionCalculator)
        );

        verify(versionCalculator).calculate(
                eq(DEFAULT_ERROR_BOOSTER_ENABLED),
                eq(Optional.of(DEFAULT_RELEASE_VERSION))
        );
    }

    @ParameterizedTest
    @EnumSource(UnifiedAgentConfigVersion.class)
    void versionCalculatorUsedTest(UnifiedAgentConfigVersion expectedVersion) {
        var versionCalculator = createVersionCalculatorMock(
                expectedVersion
        );

        var versionFactory = createVersionFactoryMock(
                DEFAULT_UNIFIED_AGENT_CONFIG
        );

        unifiedAgentConfigBuildScenario(
                DEFAULT_UNIFIED_AGENT_CONFIG_FACTORY.toBuilder()
                        .withVersionCalculator(versionCalculator)
                        .withVersionFactory(versionFactory)
        );

        verify(versionFactory).build(
                eq(expectedVersion),
                anyLong(),
                anyString(),
                anyString(),
                anyString(),
                any(ThrottlingPolicy.class),
                any(ThrottlingLimits.class),
                any(Optional.class),
                anySet(),
                any(LogbrokerTopicDescription.class)
        );
    }

    @Test
    void versionFactoryArgumentsTest() {
        var versionFactory = createVersionFactoryMock(
                DEFAULT_UNIFIED_AGENT_CONFIG
        );

        unifiedAgentConfigBuildScenario(
                DEFAULT_UNIFIED_AGENT_CONFIG_FACTORY.toBuilder()
                        .withVersionFactory(versionFactory)
        );

        verify(versionFactory).build(
                eq(DEFAULT_CONFIG_VERSION),
                eq(DEFAULT_BUILD_TASK_ID),
                eq(DEFAULT_STATIC_SECRET),
                eq(DEFAULT_STAGE_ID),
                eq(DEFAULT_DEPLOY_UNIT_ID),
                eq(DEFAULT_THROTTLING_POLICY),
                eq(DEFAULT_THROTTLING_CUSTOM_LIMITS),
                eq(Optional.of(DEFAULT_DATA_RETENTION_LIMITS)),
                eq(DEFAULT_CLUSTERS),
                eq(DEFAULT_TOPIC_DESCRIPTION)
        );
    }

    public static Stream<NamedArgument<UnifiedAgentConfig>> generateDifferentUnifiedAgentConfigArguments() {
        return IntStream.rangeClosed(1, 2).mapToObj(
                index -> NamedArgument.of(
                        "config " + index,
                        () -> toJson(Map.of("config", "" + index))
                )
        );
    }

    private static Stream<NamedArgument<UnifiedAgentConfig>> versionFactoryUsedTestParameters() {
        return generateDifferentUnifiedAgentConfigArguments();
    }

    @ParameterizedTest
    @MethodSource("versionFactoryUsedTestParameters")
    void versionFactoryUsedTest(NamedArgument<UnifiedAgentConfig> expectedConfig) {
        var versionFactory = createVersionFactoryMock(
                expectedConfig.getArgument()
        );

        var actualConfig = unifiedAgentConfigBuildScenario(
                DEFAULT_UNIFIED_AGENT_CONFIG_FACTORY.toBuilder()
                        .withVersionFactory(versionFactory)
        );

        assertThatEquals(actualConfig, expectedConfig.getArgument());
    }

    private static UnifiedAgentConfig unifiedAgentConfigBuildScenario(UnifiedAgentConfigFactory.Builder factoryBuilder) {
        return unifiedAgentConfigBuildScenario(factoryBuilder.build());
    }

    private static UnifiedAgentConfig unifiedAgentConfigBuildScenario(UnifiedAgentConfigFactory factory) {
        return factory.build(
                DEFAULT_LAYERS,
                Optional.of(DEFAULT_RESOURCE_META),
                Optional.of(DEFAULT_LOGBROKER_TOOLS_SIDECAR_INFO),
                DEFAULT_LOGBROKER_TOPIC_REQUEST,
                DEFAULT_RESOURCE_SUPPLIER,
                DEFAULT_STAGE_ID,
                DEFAULT_DEPLOY_UNIT_ID,
                DEFAULT_FULL_DEPLOY_UNIT_ID,
                DEFAULT_CLUSTERS,
                DEFAULT_LOGBROKER_TOPIC_CONFIG,
                DEFAULT_PATCHER_LIMITS_KEY,
                DEFAULT_STATIC_SECRET
        );
    }

    public static UnifiedAgentConfigFactory createUnifiedAgentConfigFactoryMock(
            UnifiedAgentConfig expectedUnifiedAgentConfig) {
        var unifiedAgentConfigFactory = mock(UnifiedAgentConfigFactory.class);

        when(unifiedAgentConfigFactory.build(
                anyCollection(),
                any(Optional.class),
                any(Optional.class),
                any(LogbrokerTopicRequest.class),
                any(ResourceSupplier.class),
                anyString(),
                anyString(),
                anyString(),
                anySet(),
                any(LogbrokerTopicConfig.class),
                anyString(),
                anyString()
        )).thenReturn(expectedUnifiedAgentConfig);

        return unifiedAgentConfigFactory;
    }
}

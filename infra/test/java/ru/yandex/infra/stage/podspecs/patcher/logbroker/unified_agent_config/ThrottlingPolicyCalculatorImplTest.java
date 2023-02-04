package ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import ru.yandex.infra.stage.TestData;
import ru.yandex.infra.stage.podspecs.SandboxResourceMeta;
import ru.yandex.infra.stage.podspecs.SandboxResourceMetaAttributesUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.yandex.infra.stage.podspecs.SandboxResourceMetaAttributesUtilsTest.createFlagCalculatorMock;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.ThrottlingPolicyCalculatorImpl.INSTANCE;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.ThrottlingPolicyCalculatorImpl.USE_YD_THROTTLING_ATTRIBUTE_KEY;
import static ru.yandex.infra.stage.podspecs.patcher.logbroker.unified_agent_config.ThrottlingPolicyCalculatorImpl.YD_THROTTLING_BEFORE_COMPRESSION_ATTRIBUTE_KEY;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

public class ThrottlingPolicyCalculatorImplTest {

    static ThrottlingPolicyCalculator createThrottlingPolicyCalculatorMock(ThrottlingPolicy throttlingPolicy) {
        var throttlingPolicyCalculator = mock(ThrottlingPolicyCalculator.class);

        when(throttlingPolicyCalculator.calculate(
                any(Optional.class))
        ).thenReturn(throttlingPolicy);

        return throttlingPolicyCalculator;
    }

    private static SandboxResourceMetaAttributesUtils.FlagCalculator createFlagCalculatorWithThrottlingFlags(
            boolean useThrottling,
            boolean throttlingBeforeCompression) {
        return createFlagCalculatorMock(
                Map.of(
                        USE_YD_THROTTLING_ATTRIBUTE_KEY, useThrottling,
                        YD_THROTTLING_BEFORE_COMPRESSION_ATTRIBUTE_KEY, throttlingBeforeCompression
                )
        );
    }

    private static final boolean DEFAULT_USE_YD_THROTTLING = true;
    private static final boolean DEFAULT_YD_THROTTLING_BEFORE_COMPRESSION = false;

    private static final SandboxResourceMeta DEFAULT_SANDBOX_META = TestData.RESOURCE_META;

    private static final ThrottlingPolicyCalculatorImpl DEFAULT_THROTTLING_POLICY_CALCULATOR = INSTANCE;

    @Test
    void flagCalculatorArgumentsTest() {
        var flagCalculator = createFlagCalculatorWithThrottlingFlags(
                DEFAULT_USE_YD_THROTTLING, DEFAULT_YD_THROTTLING_BEFORE_COMPRESSION
        );

        throttlingPolicyScenario(
                DEFAULT_THROTTLING_POLICY_CALCULATOR.toBuilder()
                    .withFlagCalculator(flagCalculator)
        );

        verify(flagCalculator).getFlagValue(
                eq(Optional.of(DEFAULT_SANDBOX_META)),
                eq(USE_YD_THROTTLING_ATTRIBUTE_KEY)
        );

        verify(flagCalculator).getFlagValue(
                eq(Optional.of(DEFAULT_SANDBOX_META)),
                eq(YD_THROTTLING_BEFORE_COMPRESSION_ATTRIBUTE_KEY)
        );
    }

    private static Stream<Arguments> flagCalculatorUsedTestParameters() {
        return Stream.of(
                Arguments.of(false, false, ThrottlingPolicy.DISABLED),
                Arguments.of(false, true, ThrottlingPolicy.DISABLED),
                Arguments.of(true, false, ThrottlingPolicy.THROTTLING_AFTER_COMPRESSION),
                Arguments.of(true, true, ThrottlingPolicy.THROTTLING_BEFORE_COMPRESSION)
        );
    }

    @ParameterizedTest
    @MethodSource("flagCalculatorUsedTestParameters")
    void flagCalculatorUsedTest(boolean expectedUseYdThrottling,
                                boolean expectedYdThrottlingBeforeCompression,
                                ThrottlingPolicy expectedThrottlingPolicy) {
        var flagCalculator = createFlagCalculatorWithThrottlingFlags(
                expectedUseYdThrottling, expectedYdThrottlingBeforeCompression
        );

        var actualThrottlingPolicy = throttlingPolicyScenario(
                DEFAULT_THROTTLING_POLICY_CALCULATOR.toBuilder()
                        .withFlagCalculator(flagCalculator)
        );

        assertThatEquals(actualThrottlingPolicy, expectedThrottlingPolicy);
    }

    private static ThrottlingPolicy throttlingPolicyScenario(ThrottlingPolicyCalculatorImpl.Builder builder) {
        return throttlingPolicyScenario(builder.build());
    }

    private static ThrottlingPolicy throttlingPolicyScenario(ThrottlingPolicyCalculator throttlingPolicyCalculator) {
        return throttlingPolicyCalculator.calculate(
                Optional.of(DEFAULT_SANDBOX_META)
        );
    }
}

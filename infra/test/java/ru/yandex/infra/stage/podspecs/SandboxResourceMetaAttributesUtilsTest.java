package ru.yandex.infra.stage.podspecs;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import one.util.streamex.EntryStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ru.yandex.infra.stage.podspecs.SandboxResourceMetaAttributesUtils.FLAG_CALCULATOR;
import static ru.yandex.infra.stage.podspecs.SandboxResourceMetaAttributesUtils.TRUE_ATTRIBUTE_VALUE;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

public class SandboxResourceMetaAttributesUtilsTest {

    private enum BooleanAttribute {
        NOT_DEFINED(false, "not defined"),
        FALSE(true, ""),
        TRUE(true, TRUE_ATTRIBUTE_VALUE);

        private final boolean defined;
        private final String value;

        BooleanAttribute(boolean defined, String value) {
            this.defined = defined;
            this.value = value;
        }

        public boolean isDefined() {
            return defined;
        }

        public String getValue() {
            return value;
        }
    }

    private static Map<String, String> toMetaAttributes(Map<String, BooleanAttribute> attributes) {
        return EntryStream.of(attributes)
                .filterValues(BooleanAttribute::isDefined)
                .mapValues(BooleanAttribute::getValue)
                .toMap();
    }

    private static SandboxResourceMeta createMetaMock(Map<String, BooleanAttribute> metaBooleanAttributes) {
        var metaStringAttributes = toMetaAttributes(metaBooleanAttributes);

        var meta = mock(SandboxResourceMeta.class);
        when(meta.getAttributes()).thenReturn(metaStringAttributes);
        return meta;
    }

    private static final String DEFAULT_FLAG_NAME = "flag_name";

    private static Stream<Arguments> flagValueWhenMetaPresentedTestParameters() {
        return Stream.of(
            Arguments.of(BooleanAttribute.TRUE, true),
            Arguments.of(BooleanAttribute.FALSE, false),
            Arguments.of(BooleanAttribute.NOT_DEFINED, false)
        );
    }

    @ParameterizedTest
    @MethodSource("flagValueWhenMetaPresentedTestParameters")
    void flagValueWhenMetaPresentedTest(BooleanAttribute flagAttribute,
                                        boolean expectedFlagValue) {
        var metaAttributes = Map.of(DEFAULT_FLAG_NAME, flagAttribute);
        var meta = createMetaMock(metaAttributes);

        getFlagValueScenario(Optional.of(meta), expectedFlagValue);
    }

    @Test
    void flagValueIsFalseWhenEmptyMetaTest() {
        getFlagValueScenario(Optional.empty(), false);
    }

    private static void getFlagValueScenario(Optional<SandboxResourceMeta> meta,
                                             boolean expectedFlagValue) {
        var actualFlagValue = FLAG_CALCULATOR.getFlagValue(meta, DEFAULT_FLAG_NAME);
        assertThatEquals(actualFlagValue, expectedFlagValue);
    }

    public static SandboxResourceMetaAttributesUtils.FlagCalculator createFlagCalculatorMock(
            Map<String, Boolean> expectedFlagValues) {

        var flagCalculator = mock(SandboxResourceMetaAttributesUtils.FlagCalculator.class);

        EntryStream.of(expectedFlagValues)
                .forKeyValue((name, value) ->
                        when(flagCalculator.getFlagValue(
                            any(Optional.class),
                            eq(name)
                        )
                ).thenReturn(value));

        return flagCalculator;
    }
}

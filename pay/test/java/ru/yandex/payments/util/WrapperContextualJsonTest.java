package ru.yandex.payments.util;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.serialize.ObjectSerializer;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.val;
import net.javacrumbs.jsonunit.core.Option;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.payments.util.Wrappers.BooleanValue;
import ru.yandex.payments.util.Wrappers.IntValue;
import ru.yandex.payments.util.Wrappers.LongValue;
import ru.yandex.payments.util.Wrappers.ShortValue;
import ru.yandex.payments.util.Wrappers.StringValue;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class WrapperContextualJsonTest {
    @Inject
    ObjectSerializer serializer;

    @Introspected
    static record SerializableValue(BooleanValue booleanValue,
                                    ShortValue shortValue,
                                    IntValue intValue,
                                    StringValue strValue,
                                    LongValue longValue,
                                    Optional<ShortValue> shortValueOptional,
                                    Optional<IntValue> intValueEmptyOptional,
                                    List<StringValue> stringValueList,
                                    List<BooleanValue> booleanValueEmptyList,
                                    Set<LongValue> longValueSet,
                                    Set<IntValue> intValueEmptySet,
                                    Optional<Set<LongValue>> optionalLongValueSet) {
        SerializableValue(BooleanValue booleanValue,
                          ShortValue shortValue,
                          IntValue intValue,
                          StringValue strValue,
                          LongValue longValue,
                          ShortValue shortValueOptional,
                          List<StringValue> stringValueList,
                          Set<LongValue> longValueSet) {
            this(booleanValue, shortValue, intValue, strValue, longValue, Optional.of(shortValueOptional),
                    Optional.empty(), stringValueList, emptyList(), longValueSet, emptySet(),
                    Optional.of(longValueSet));
        }
    }

    @Test
    @DisplayName("Verify that wrapper types serialization/deserialization works as expected")
    void testSerde() {
        val expected = new SerializableValue(
                new BooleanValue(true),
                new ShortValue((short) 0),
                new IntValue(1),
                new StringValue("2"),
                new LongValue(3L),

                new ShortValue((short) 4),
                List.of(new StringValue("5"), new StringValue("6")),
                Set.of(new LongValue(7L), new LongValue(8L))
        );

        val dataOpt = serializer.serialize(expected);
        assertThat(dataOpt)
                .isPresent();
        val data = dataOpt.orElseThrow();

        val dataJson = new String(data, StandardCharsets.UTF_8);
        assertThatJson(dataJson)
                .when(Option.IGNORING_ARRAY_ORDER)
                .isEqualTo(
                        // CHECKSTYLE:OFF
                        // language=json
                        """
                        {
                            "booleanValue": true,
                            "shortValue": 0,
                            "intValue": 1,
                            "strValue": "2",
                            "longValue": 3,
                            "shortValueOptional": 4,
                            "intValueEmptyOptional": null,
                            "stringValueList": ["5", "6"],
                            "booleanValueEmptyList": [],
                            "longValueSet": [7, 8],
                            "intValueEmptySet": [],
                            "optionalLongValueSet": [7, 8]
                        }
                        """
                        // CHECKSTYLE:ON
                );

        val actual = serializer.deserialize(data, SerializableValue.class);
        assertThat(actual)
                .isPresent()
                .contains(expected);
    }
}

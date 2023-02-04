package ru.yandex.payments.micronaut_logbroker;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import io.micronaut.core.annotation.Creator;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.type.Argument;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.payments.micronaut_logbroker.annotations.Tskv;
import ru.yandex.payments.micronaut_logbroker.serde.SerdeRegistry;
import ru.yandex.payments.util.tskv.TskvParser;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class SerdeTest {
    @Inject
    SerdeRegistry serdeRegistry;

    @Test
    @DisplayName("Verify that byte array serde could serialize/deserialize byte arrays")
    void testByteArraySerde() {
        val foundSerde = serdeRegistry.findSerde(Argument.of(byte[].class));
        assertThat(foundSerde)
                .isPresent();

        val serde = foundSerde.orElseThrow();
        val value = new byte[]{(byte) 0xDE, (byte) 0xAD, (byte) 0xBE, (byte) 0xEF};

        assertThat(serde.serialize(value))
                .isEqualTo(value);
        assertThat(serde.deserialize(value))
                .isEqualTo(value);
    }

    @Introspected
    public static record JsonPojo(String str,
                                  int num) {
    }

    @Test
    @DisplayName("Verify that json serde could serialize/deserialize @Introspected pojos")
    void testJsonSerde() {
        val foundSerde = serdeRegistry.findSerde(Argument.of(JsonPojo.class));
        assertThat(foundSerde)
                .isPresent();

        val serde = foundSerde.orElseThrow();
        val value = new JsonPojo("string", 42);
        val jsonValue = """
                {"str": "string", "num": 42}""";

        assertThatJson(new String(serde.serialize(value)))
                .isEqualTo(jsonValue);
        assertThat(serde.deserialize(jsonValue.getBytes()))
                .isEqualTo(value);
    }

    @Tskv
    @Introspected
    public static record TskvPojo(String message,
                                  boolean flag,
                                  int num) {
        @Creator
        public TskvPojo {
        }
    }

    @Test
    @DisplayName("Verify that tskv serde could serialize/deserialize @Introspected @Tskv pojos")
    void testTskvSerde() {
        val foundSerde = serdeRegistry.findSerde(Argument.of(TskvPojo.class));
        assertThat(foundSerde)
                .isPresent();

        val serde = foundSerde.orElseThrow();
        val value = new TskvPojo("test", true, 42);
        val tskvValue = "tskv\tflag=true\tmessage=test\tnum=42";
        val tskvMap = Map.of(
                "message", "test",
                "flag", "true",
                "num", "42"
        );

        assertThat(TskvParser.parseLine(new String(serde.serialize(value))))
                .isEqualTo(tskvMap);
        assertThat(serde.deserialize(tskvValue.getBytes()))
                .isEqualTo(value);
    }

    @Test
    @SuppressWarnings("unchecked")
    @DisplayName("Verify that tskv serde could serialize/deserialize collection of @Introspected @Tskv pojos")
    void testTskvCollectionSerde() {
        final var foundSerde = serdeRegistry.findSerde(Argument.of(List.class, TskvPojo.class));
        assertThat(foundSerde)
                .isPresent();

        final var serde = foundSerde.orElseThrow();
        val value = List.of(
                new TskvPojo("msg1", true, 0),
                new TskvPojo("msg2", false, 1)
        );

        val tskvValue = """
                tskv\tflag=true\tmessage=msg1\tnum=0
                tskv\tflag=false\tmessage=msg2\tnum=1""";
        val tskvMaps = List.of(
                Map.of(
                        "message", "msg1",
                        "flag", "true",
                        "num", "0"
                ),
                Map.of(
                        "message", "msg2",
                        "flag", "false",
                        "num", "1"
                )
        );

        assertThat(TskvParser.parseLines(new String(serde.serialize(value))))
                .containsExactlyInAnyOrderElementsOf(tskvMaps);
        assertThat((List<TskvPojo>) serde.deserialize(tskvValue.getBytes()))
                .isEqualTo(value);
    }
}

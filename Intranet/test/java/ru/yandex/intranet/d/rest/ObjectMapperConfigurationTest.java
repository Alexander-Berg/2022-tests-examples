package ru.yandex.intranet.d.rest;

import java.time.Instant;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import ru.yandex.intranet.d.IntegrationTest;

/**
 * Tests for configuration default ObjectMapper
 *
 * @author Evgenii Serov <evserov@yandex-team.ru>
 */
@IntegrationTest
public class ObjectMapperConfigurationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void objectMapperIgnoreUnknownPropertiesTest() {
        String json = """
                {
                    "id": "value",
                    "timestamp": "1970-01-01T00:00:05Z",
                    "unknownProperty": "value"
                }
                """;
        Assertions.assertDoesNotThrow(() -> objectMapper.readerFor(TestDto.class).readValue(json));
    }

    @Test
    public void objectMapperWriteDatesAsIsoTest() throws JsonProcessingException {
        TestDto testDto = new TestDto("1", Instant.ofEpochSecond(5L));
        String json = objectMapper.writerFor(TestDto.class).writeValueAsString(testDto);
        Assertions.assertEquals("{\"id\":\"1\",\"timestamp\":\"1970-01-01T00:00:05Z\"}", json);
    }

    private static class TestDto {
        private final String id;
        private final Instant timestamp;

        @JsonCreator
        TestDto(String id, Instant timestamp) {
            this.id = id;
            this.timestamp = timestamp;
        }

        public String getId() {
            return id;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            TestDto testDto = (TestDto) o;
            return Objects.equals(id, testDto.id) && Objects.equals(timestamp, testDto.timestamp);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, timestamp);
        }

        @Override
        public String toString() {
            return ReflectionToStringBuilder.toString(this);
        }
    }

}

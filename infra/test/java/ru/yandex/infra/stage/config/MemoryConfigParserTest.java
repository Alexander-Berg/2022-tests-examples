package ru.yandex.infra.stage.config;

import java.util.Map;

import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.infra.stage.podspecs.PodSpecUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static ru.yandex.infra.stage.util.AssertUtils.assertThatEquals;

public class MemoryConfigParserTest {

    @ParameterizedTest
    @CsvSource({
            "10," + 10,
            "1234kb," + 1234 * PodSpecUtils.KILOBYTE,
            "30mb," + 30 * PodSpecUtils.MEGABYTE,
            "4gb," + 4 * PodSpecUtils.GIGABYTE
    })
    public void parseMemoryCorrectTest(String memoryString,
                                       long expectedMemory) {
        long actualMemory = MemoryConfigParser.parseMemory(memoryString);
        assertThatEquals(actualMemory, expectedMemory);
    }

    @ParameterizedTest
    @ValueSource(strings = { "10b", "5tb", "kb", "not_memory"})
    public void parseMemoryIncorrectTest(String incorrectMemoryString) {
        assertThrows(
                RuntimeException.class,
                () -> MemoryConfigParser.parseMemory(incorrectMemoryString)
        );
    }

    @Test
    public void parseMemoryFromCorrectTest() {
        String memoryString = "5mb";
        String memoryKey = "memoryKey";

        long expectedMemory = MemoryConfigParser.parseMemory(memoryString);
        long actualMemory = MemoryConfigParser.parseMemoryFrom(
                ConfigFactory.parseMap(Map.of(
                        memoryKey, memoryString
                )),
                memoryKey
        );

        assertThatEquals(actualMemory, expectedMemory);
    }

    @Test
    public void parseMemoryFromMissingTest() {
        assertThrows(
                ConfigException.Missing.class,
                () -> MemoryConfigParser.parseMemoryFrom(
                        ConfigFactory.empty(),
                        "not_found_key"
                )
        );
    }
}

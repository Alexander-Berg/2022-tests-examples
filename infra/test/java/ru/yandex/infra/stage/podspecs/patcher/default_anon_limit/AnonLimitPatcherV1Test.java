package ru.yandex.infra.stage.podspecs.patcher.default_anon_limit;

import java.util.OptionalLong;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

public class AnonLimitPatcherV1Test extends AnonLimitPatcherBaseTest {

    @ParameterizedTest
    @CsvSource({
            "2000000000, 1865782272", //podMemory - DEFAULT_FILE_CACHE_RESERVED_MEMORY_SIZE
            "1000000000, 865782272", //podMemory - DEFAULT_FILE_CACHE_RESERVED_MEMORY_SIZE
            "500000000, 365782272", //podMemory - DEFAULT_FILE_CACHE_RESERVED_MEMORY_SIZE
            "100000000, 0", //podMemory < 128M, skip limit
            "50000000, 0", //podMemory < 128M, skip limit
    })
    void setDefaultAnonLimitTest(long podMemory, long expectedDefaultAnonLimit) {
        anonymousMemoryLimitScenario(podMemory, OptionalLong.empty(), expectedDefaultAnonLimit);
    }
}

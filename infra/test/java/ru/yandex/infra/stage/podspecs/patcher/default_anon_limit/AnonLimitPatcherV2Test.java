package ru.yandex.infra.stage.podspecs.patcher.default_anon_limit;

import java.util.OptionalLong;
import java.util.function.Function;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import ru.yandex.infra.stage.podspecs.SpecPatcher;
import ru.yandex.yp.client.api.TPodTemplateSpec;

public class AnonLimitPatcherV2Test extends AnonLimitPatcherBaseTest {

    @Override
    protected Function<AnonLimitPatcherContext, ? extends SpecPatcher<TPodTemplateSpec.Builder>> getPatcherConstructor() {
        return AnonLimitPatcherV2::new;
    }

    @ParameterizedTest
    @CsvSource({
            "2000000000, 1865782272", //podMemory - DEFAULT_FILE_CACHE_RESERVED_MEMORY_SIZE
            "1000000000, 900000000", //podMemory - podMemory * 10%
            "500000000, 450000000", //podMemory - podMemory * 10%
            "100000000, 90000000", //podMemory - podMemory * 10%
            "50000000, 45000000", //podMemory - podMemory * 10%
    })
    void setDefaultAnonLimitTest(long podMemory, long expectedDefaultAnonLimit) {
        anonymousMemoryLimitScenario(podMemory, OptionalLong.empty(), expectedDefaultAnonLimit);
    }

}

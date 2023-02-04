package ru.yandex.payments.tvmlocal.testing;

import java.util.OptionalInt;

import lombok.val;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.payments.tvmlocal.testing.options.ConfigLocation;
import ru.yandex.payments.tvmlocal.testing.options.Mode;
import ru.yandex.payments.tvmlocal.testing.options.ResourceConfigLocation;
import ru.yandex.payments.tvmlocal.testing.options.TvmToolOptions;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

class LocalTvmTest {
    private static final BinarySource TVMTOOL_BINARY_SOURCE = new BinarySandboxSource();
    private static final ConfigLocation CONFIG_LOCATION = new ResourceConfigLocation("tvmtool.conf");
    private static final TvmToolOptions OPTIONS = new TvmToolOptions(OptionalInt.empty(),
            CONFIG_LOCATION, Mode.UNITTEST,
            emptyMap(), TvmToolOptions.generateAuthToken());

    @Test
    @DisplayName("Verify that tvmtool could be started and stopped using config from classpath resources")
    void testRunWithConfigFromResources() {
        val tool = TvmTool.start(TVMTOOL_BINARY_SOURCE, OPTIONS);
        assertThat(tool.ping())
                .describedAs("Check tvmtool ping")
                .isTrue();
        tool.stop();
    }
}

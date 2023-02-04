package ru.yandex.payments.tvmlocal.testing;

import javax.inject.Inject;

import io.micronaut.context.annotation.Value;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class MicronautIntegrationTest {
    @Value("${tvmlocal.tvmtool-port}")
    int tvmToolPort;

    @Inject
    TvmTool tool;

    @Test
    @DisplayName("Verify that tvmtool is already running before the test execution")
    void testTvmToolIsRunning() {
        assertThat(tool.ping())
                .describedAs("Check tvmtool ping")
                .isTrue();

        assertThat(tool.getPort())
                .isEqualTo(tvmToolPort);
    }
}

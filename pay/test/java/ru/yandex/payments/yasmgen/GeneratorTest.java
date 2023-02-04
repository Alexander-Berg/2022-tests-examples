package ru.yandex.payments.yasmgen;

import javax.inject.Inject;
import javax.validation.constraints.NotBlank;

import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ru.yandex.payments.yasmgen.configuration.Panels;

import static org.assertj.core.api.Assertions.assertThat;

@MicronautTest
class GeneratorTest {
    @Client("/")
    @MonitoringGroup("test-self")
    interface SelfClient {
        @Get("/yasm/generate")
        Panel.ChartPanel generateChart(@QueryValue @NotBlank String panel);

        @Get("/yasm/generate")
        Panel.AlertPanel generateAlert(@QueryValue @NotBlank String panel);
    }

    @Inject
    SelfClient selfClient;

    @ParameterizedTest
    @ValueSource(strings = {Panels.BRIEF, Panels.JVM, Panels.CLIENTS, Panels.API})
    void testChartGeneration(String panelName) {
        val panel = selfClient.generateChart(panelName);
        assertThat(panel.name())
                .isEqualTo(panelName);
        assertThat(panel.content())
                .isNotBlank();
    }

    @Test
    void testAlertGeneration() {
        val panel = selfClient.generateAlert(Panels.ALERT);
        assertThat(panel.name())
                .isEqualTo(Panels.ALERT);
        assertThat(panel.content())
                .isNotBlank();
    }
}

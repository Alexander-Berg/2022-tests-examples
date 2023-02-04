package ru.yandex.qe.http.telemetry;

import java.io.IOException;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import ru.yandex.monlib.metrics.labels.Labels;
import ru.yandex.monlib.metrics.registry.MetricId;
import ru.yandex.monlib.metrics.registry.MetricRegistry;

/**
 * Established by terry
 * on 16.03.14.
 */
public class MetricsHttpClientBuilderTest {

    @Test
    public void connection_pool_wrapped_with_gauges() {
        final MetricRegistry metricRegistry = new MetricRegistry();
        Assertions.assertNull(metricRegistry.getMetric(new MetricId("http_client.received_bytes",
                Labels.of("http_client_label", "label"))));

        new MetricsInstrumentedHttpClientBuilder("label", metricRegistry).build();

        Assertions.assertNotNull(metricRegistry.getMetric(new MetricId("http_client.received_bytes",
                Labels.of("http_client_label", "label"))));
    }

    @Test
    public void execution_wrapped_with_meter_aspect() {
        final MetricRegistry metricRegistry = new MetricRegistry();
        Assertions.assertNull(metricRegistry.getMetric(new MetricId("http_client.received_bytes",
                Labels.of("http_client_label", "label"))));

        final CloseableHttpClient build = new MetricsInstrumentedHttpClientBuilder("label", metricRegistry).build();
        try {
            build.execute(new HttpGet("http://localhost:12345/"));
        } catch (IOException e) {
        }

        Assertions.assertNotNull(metricRegistry.getMetric(new MetricId("http_client.received_bytes",
                Labels.of("http_client_label", "label"))));
    }
}

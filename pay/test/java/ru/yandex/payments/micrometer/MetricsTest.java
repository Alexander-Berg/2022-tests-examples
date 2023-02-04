package ru.yandex.payments.micrometer;

import java.util.List;

import javax.inject.Inject;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.payments.micrometer.unistat.UnistatSlaConfiguration;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.json;
import static net.javacrumbs.jsonunit.jsonpath.JsonPathAdapter.inPath;
import static org.assertj.core.api.Assertions.assertThat;

@Client("/")
interface TestClient {
    @Get("/test/dummy")
    String foo();

    @Post("/test/some/path")
    int bar();

    @Get("/test/path/{var}")
    String path(@PathVariable String var);

    @Get("/unknown")
    String unknown();

    @Get("/unistat")
    @Produces(MediaType.TEXT_JSON)
    String unistat();
}

@Controller("/test")
class TestController {
    @Get("/dummy")
    public String foo() {
        return "foo";
    }

    @Post("/some/path")
    public int bar() {
        return 1;
    }

    @Get("/path/{var}")
    public String path(@PathVariable String var) {
        return var;
    }
}

@MicronautTest
class MetricsTest {
    @Inject
    TestClient client;

    @Inject
    List<UnistatSlaConfiguration> slaConfigs;

    private static void assertIsSingleValueHistogram(Object element, String signal) {
        assertThatJson(inPath(element, "$[0]"))
                .isEqualTo(signal);
        assertThatJson(inPath(element, "$[1]"))
                .isArray()
                .anySatisfy(nested -> {
                    assertThatJson(inPath(nested, "$[1]"))
                            .isEqualTo(1);
                });
    }

    @Test
    @SneakyThrows
    @DisplayName("Verify that micronaut-micrometer collects controller metrics")
    void httpMetricsTest() {
        assertThat(client.foo())
                .isEqualTo("foo");
        assertThat(client.bar())
                .isEqualTo(1);
        assertThat(client.unknown())
                .isNull();
        assertThat(client.path("value"))
                .isEqualTo("value");

        assertThatJson(client.unistat()).and(
                a -> a.node("")
        );

        assertThatJson(client.unistat())
                .isArray()
                .anySatisfy(elem -> assertIsSingleValueHistogram(elem,
                        "http_server_requests_get_2xx_test_dummy_time_hgram"))
                .anySatisfy(elem -> assertIsSingleValueHistogram(elem,
                        "http_server_requests_post_2xx_test_some_path_time_hgram"))
                .anySatisfy(elem -> assertIsSingleValueHistogram(elem,
                        "http_server_requests_get_2xx_test_path_var_time_hgram"))
                .anySatisfy(elem -> assertIsSingleValueHistogram(elem,
                        "http_client_requests_get_2xx_test_dummy_time_hgram"))
                .anySatisfy(elem -> assertIsSingleValueHistogram(elem,
                        "http_client_requests_post_2xx_test_some_path_time_hgram"))
                .contains(
                        json("[\"http_server_requests_4xx_not_found_count_summ\", 1]"),
                        json("[\"http_client_requests_get_4xx_not_found_count_summ\", 1]"),

                        json("[\"http_server_requests_get_2xx_test_dummy_count_summ\", 1]"),
                        json("[\"http_server_requests_post_2xx_test_some_path_count_summ\", 1]"),

                        json("[\"http_client_requests_get_2xx_test_dummy_count_summ\", 1]"),
                        json("[\"http_client_requests_post_2xx_test_some_path_count_summ\", 1]"),

                        json("[\"http_server_requests_get_2xx_test_path_var_count_summ\", 1]"),
                        json("[\"http_client_requests_get_2xx_test_path_var_count_summ\", 1]")
                );
    }
}

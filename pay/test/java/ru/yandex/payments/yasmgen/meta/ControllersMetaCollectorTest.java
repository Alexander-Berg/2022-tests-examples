package ru.yandex.payments.yasmgen.meta;

import javax.inject.Inject;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.CustomHttpMethod;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Head;
import io.micronaut.http.annotation.Options;
import io.micronaut.http.annotation.Patch;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.annotation.Trace;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.payments.yasmgen.CritThreshold;
import ru.yandex.payments.yasmgen.MonitoringGroup;
import ru.yandex.payments.yasmgen.WarnThreshold;
import ru.yandex.payments.yasmgen.configuration.AlertRange;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.payments.yasmgen.meta.EndpointType.SERVER;

@MicronautTest
class ControllersMetaCollectorTest {
    @Controller
    @MonitoringGroup("test-group")
    @WarnThreshold(lower = "10", upper = "15")
    @CritThreshold(lower = "20", upper = "100")
    static class TestController {
        @Get("/test/basic_endpoint")
        public String basicEndpoint() {
            return "basic";
        }

        @Post(uris = {"/test/multi1_endpoint", "/multi2_endpoint"})
        public String multiEndpoint(@Body String body) {
            return "multi";
        }

        @CustomHttpMethod(uri = "/test/uri_template_endpoint/{pathVar}/suffix", method = "TEST")
        public String uriTemplateEndpoint(@PathVariable String pathVar) {
            return "uri_template=" + pathVar;
        }

        public String justAPublicMethod() {
            return basicEndpoint() + "-public";
        }
    }

    @Controller("/prefix")
    static class CommonPrefixTestController {
        @Head("/head")
        @PredefinedWarnThreshold
        public String head(@QueryValue String param) {
            return "endpoint";
        }

        @Options("/options")
        public String options() {
            return "endpoint";
        }

        @Trace("/trace")
        @WarnThreshold(lower = "1", upper = "${warn-upper-threshold}")
        @CritThreshold(lower = "3")
        public String trace() {
            return "endpoint";
        }

        @Put("/put")
        public String put() {
            return "endpoint";
        }

        @Patch("/patch")
        public String patch() {
            return "endpoint";
        }

        @Delete("/delete")
        public String delete() {
            return "endpoint";
        }

        private String justAPrivateMethod() {
            return options() + "-private";
        }
    }

    @Inject
    private ControllersMetaCollector collector;

    @Value("${warn-upper-threshold}")
    int warnUpperThreshold;

    @Test
    @DisplayName("Verify that ControllersMetaCollector collects meta for all of the HTTP controllers")
    void testCollect() {
        assertThat(collector.controllers())
                .containsOnlyKeys("test-group", "default", "yasm")
                .hasEntrySatisfying(
                        "test-group",
                        endpointSet -> {
                            assertThat(endpointSet.name())
                                    .isEqualTo("test-group");
                            assertThat(endpointSet.endpoints())
                                    .containsExactlyInAnyOrder(
                                            new Endpoint(
                                                    SERVER,
                                                    "/test/basic_endpoint",
                                                    "GET",
                                                    AlertRange.of(10, 15, 20, 100)
                                            ),
                                            new Endpoint(
                                                    SERVER,
                                                    "/test/multi1_endpoint",
                                                    "POST",
                                                    AlertRange.of(10, 15, 20, 100)
                                            ),
                                            new Endpoint(
                                                    SERVER,
                                                    "/multi2_endpoint",
                                                    "POST",
                                                    AlertRange.of(10, 15, 20, 100)
                                            ),
                                            new Endpoint(
                                                    SERVER,
                                                    "/test/uri_template_endpoint/{pathVar}/suffix",
                                                    "TEST",
                                                    AlertRange.of(10, 15, 20, 100)
                                            )
                                    );
                        }
                )
                .hasEntrySatisfying(
                        "default",
                        endpointSet -> {
                            assertThat(endpointSet.name())
                                    .isEqualTo("default");
                            assertThat(endpointSet.endpoints())
                                    .containsExactlyInAnyOrder(
                                            new Endpoint(
                                                    SERVER,
                                                    "/prefix/head",
                                                    "HEAD",
                                                    AlertRange.of(0, warnUpperThreshold, null, null)
                                            ),
                                            new Endpoint(
                                                    SERVER,
                                                    "/prefix/options",
                                                    "OPTIONS",
                                                    AlertRange.NONE
                                            ),
                                            new Endpoint(
                                                    SERVER,
                                                    "/prefix/trace",
                                                    "TRACE",
                                                    AlertRange.of(1, warnUpperThreshold, 3, null)
                                            ),
                                            new Endpoint(
                                                    SERVER,
                                                    "/prefix/put",
                                                    "PUT",
                                                    AlertRange.NONE
                                            ),
                                            new Endpoint(
                                                    SERVER,
                                                    "/prefix/patch",
                                                    "PATCH",
                                                    AlertRange.NONE
                                            ),
                                            new Endpoint(
                                                    SERVER,
                                                    "/prefix/delete",
                                                    "DELETE",
                                                    AlertRange.NONE
                                            )
                                    );
                        }
                )
                .hasEntrySatisfying(
                        "yasm",
                        endpointSet -> {
                            assertThat(endpointSet.name())
                                    .isEqualTo("yasm");
                            assertThat(endpointSet.endpoints())
                                    .containsExactlyInAnyOrder(
                                            new Endpoint(
                                                    SERVER,
                                                    "/yasm/generate",
                                                    "GET",
                                                    AlertRange.of(11, 22, 33, null)
                                            ),
                                            new Endpoint(
                                                    SERVER,
                                                    "/yasm/deploy/{panel}",
                                                    "POST",
                                                    AlertRange.of(11, 22, 33, null)
                                            ),
                                            new Endpoint(
                                                    SERVER,
                                                    "/yasm/deploy/all",
                                                    "POST",
                                                    AlertRange.of(11, 22, 33, null)
                                            ),
                                            new Endpoint(
                                                    SERVER,
                                                    "/yasm/meta/endpoints",
                                                    "GET",
                                                    AlertRange.of(11, 22, 33, null)
                                            )
                                    );
                        }
                );
    }
}

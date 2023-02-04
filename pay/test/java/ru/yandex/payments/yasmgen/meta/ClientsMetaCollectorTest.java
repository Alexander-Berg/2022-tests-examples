package ru.yandex.payments.yasmgen.meta;

import javax.inject.Inject;

import io.micronaut.context.annotation.Value;
import io.micronaut.http.annotation.Body;
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
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ru.yandex.payments.yasmgen.CritThreshold;
import ru.yandex.payments.yasmgen.MonitoringGroup;
import ru.yandex.payments.yasmgen.WarnThreshold;
import ru.yandex.payments.yasmgen.configuration.AlertRange;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.payments.yasmgen.meta.EndpointType.CLIENT;

@MicronautTest
class ClientsMetaCollectorTest {
    @Client(path = "/")
    @MonitoringGroup("test-group")
    @WarnThreshold(lower = "10", upper = "15")
    @CritThreshold(lower = "20", upper = "100")
    interface TestClient {
        @Get("/test/basic_endpoint")
        String basic();

        @CustomHttpMethod(uri = "/test/uri_template_endpoint/{pathVar}/suffix", method = "TEST")
        String uriTemplate(@PathVariable String pathVar);

        default String defaultMethod() {
            return basic() + "-default";
        }
    }

    @Client(path = "/prefix")
    @MonitoringGroup("test-group")
    interface CommonPrefixClient {
        @Get("/one")
        @WarnThreshold(lower = "10", upper = "${warn-upper-threshold}")
        @CritThreshold(lower = "20")
        String one();

        @Post("/two")
        @WarnThreshold(upper = "3")
        @CritThreshold(lower = "4", upper = "5")
        String two(@Body String body);
    }

    @Client(id = "test-client-id")
    interface TestClientWithId {
        @Head("/head")
        String head(@QueryValue String param);

        @Delete("/delete")
        @PredefinedWarnThreshold
        String delete();

        @Options("/options")
        String options();

        @Put("/put")
        String put();

        @Patch("/patch")
        String patch();

        @Trace("/trace")
        String trace();
    }

    @Inject
    ClientsMetaCollector collector;

    @Value("${warn-upper-threshold}")
    int warnUpperThreshold;

    @Test
    @DisplayName("Verify that ClientsMetaCollector collects meta for all of the HTTP clients")
    void testCollection() {
        assertThat(collector.clients())
                .containsOnlyKeys("test-group", "test-client-id", "test-self", "yasm")
                .hasEntrySatisfying(
                        "test-group",
                        endpointSet -> {
                            assertThat(endpointSet.name())
                                    .isEqualTo("test-group");
                            assertThat(endpointSet.endpoints())
                                    .containsExactlyInAnyOrder(
                                            new Endpoint(
                                                    CLIENT,
                                                    "/test/basic_endpoint",
                                                    "GET",
                                                    AlertRange.of(10, 15, 20, 100)
                                            ),
                                            new Endpoint(
                                                    CLIENT,
                                                    "/test/uri_template_endpoint/{pathVar}/suffix",
                                                    "TEST",
                                                    AlertRange.of(10, 15, 20, 100)
                                            ),
                                            new Endpoint(
                                                    CLIENT,
                                                    "/prefix/one",
                                                    "GET",
                                                    AlertRange.of(10, warnUpperThreshold, 20, null)
                                            ),
                                            new Endpoint(
                                                    CLIENT,
                                                    "/prefix/two",
                                                    "POST",
                                                    AlertRange.of(null, 3, 4, 5)
                                            )
                                    );
                        }
                )
                .hasEntrySatisfying(
                        "test-client-id",
                        endpointSet -> {
                            assertThat(endpointSet.name())
                                    .isEqualTo("test-client-id");
                            assertThat(endpointSet.endpoints())
                                    .containsExactlyInAnyOrder(
                                            new Endpoint(
                                                    CLIENT,
                                                    "/head",
                                                    "HEAD",
                                                    AlertRange.NONE
                                            ),
                                            new Endpoint(
                                                    CLIENT,
                                                    "/delete",
                                                    "DELETE",
                                                    AlertRange.of(0, warnUpperThreshold, null, null)
                                            ),
                                            new Endpoint(
                                                    CLIENT,
                                                    "/options",
                                                    "OPTIONS",
                                                    AlertRange.NONE
                                            ),
                                            new Endpoint(
                                                    CLIENT,
                                                    "/put",
                                                    "PUT",
                                                    AlertRange.of(null, 5, null, 50)
                                            ),
                                            new Endpoint(
                                                    CLIENT,
                                                    "/patch",
                                                    "PATCH",
                                                    AlertRange.NONE
                                            ),
                                            new Endpoint(
                                                    CLIENT,
                                                    "/trace",
                                                    "TRACE",
                                                    AlertRange.NONE
                                            )
                                    );
                        }
                )
                .hasEntrySatisfying(
                        "test-self",
                        endpointSet -> {
                            assertThat(endpointSet.name())
                                    .isEqualTo("test-self");
                            assertThat(endpointSet.endpoints())
                                    .containsExactlyInAnyOrder(
                                            new Endpoint(
                                                    CLIENT,
                                                    "/yasm/generate",
                                                    "GET",
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
                                                    CLIENT,
                                                    "/srvambry/tmpl/alerts/create",
                                                    "POST",
                                                    AlertRange.of(1, 10, 10, null)
                                            ),
                                            new Endpoint(
                                                    CLIENT,
                                                    "/srvambry/tmpl/alerts/update",
                                                    "POST",
                                                    AlertRange.of(1, 10, 10, null)
                                            ),
                                            new Endpoint(
                                                    CLIENT,
                                                    "/srvambry/tmpl/alerts/apply/{alert}",
                                                    "POST",
                                                    AlertRange.of(1, 10, 10, null)
                                            ),
                                            new Endpoint(
                                                    CLIENT,
                                                    "/srvambry/tmpl/panels/create",
                                                    "POST",
                                                    AlertRange.of(1, 10, 10, null)
                                            ),
                                            new Endpoint(
                                                    CLIENT,
                                                    "/srvambry/tmpl/panels/update/content",
                                                    "POST",
                                                    AlertRange.of(1, 10, 10, null)
                                            )
                                    );
                        }
                );
    }
}

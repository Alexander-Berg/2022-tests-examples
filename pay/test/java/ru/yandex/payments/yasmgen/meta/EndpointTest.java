package ru.yandex.payments.yasmgen.meta;

import lombok.val;
import org.junit.jupiter.api.Test;

import ru.yandex.payments.yasmgen.configuration.AlertRange;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.yandex.payments.yasmgen.meta.EndpointType.CLIENT;
import static ru.yandex.payments.yasmgen.meta.EndpointType.SERVER;

class EndpointTest {
    @Test
    void simpleNormalizationTest() {
        val endpoint = new Endpoint(SERVER, "/foo/bar", "GET", AlertRange.NONE);
        assertThat(endpoint.normalize())
                .isEqualTo(new NormalizedEndpoint("foo_bar", "get", "http_server_requests", AlertRange.NONE));
    }

    @Test
    void pathVariableNormalizationTest() {
        val endpoint = new Endpoint(CLIENT, "/foo/bar/{var}", "POST", AlertRange.NONE);
        assertThat(endpoint.normalize())
                .isEqualTo(new NormalizedEndpoint("foo_bar_var", "post", "http_client_requests", AlertRange.NONE));
    }
}

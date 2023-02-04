package ru.yandex.intranet.d.web.sensors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.web.MockUser;

/**
 * Sensors API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class SensorsApiTest {

    @Autowired
    private WebTestClient webClient;
    @Value("${solomon.tvmSourceId}")
    private long solomonTvmSourceId;

    @Test
    public void getSensorsTest() {
        // TODO Actually test implementation
        webClient
                .mutateWith(MockUser.tvm(solomonTvmSourceId))
                .get()
                .uri("/sensors/metrics")
                .accept(MediaType.parseMediaType("application/x-solomon-spack"))
                .exchange()
                .expectStatus()
                .isOk();
    }

    @Test
    public void getSensorsForbiddenTest() {
        webClient
                .mutateWith(MockUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .get()
                .uri("/sensors/metrics")
                .accept(MediaType.parseMediaType("application/x-solomon-spack"))
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    public void getSensorsUnauthorizedTest() {
        webClient
                .get()
                .uri("/sensors/metrics")
                .accept(MediaType.parseMediaType("application/x-solomon-spack"))
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

}

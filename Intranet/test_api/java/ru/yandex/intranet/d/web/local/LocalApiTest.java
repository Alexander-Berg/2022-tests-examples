package ru.yandex.intranet.d.web.local;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.web.MockUser;

/**
 * Local API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class LocalApiTest {

    @Autowired
    private WebTestClient webClient;

    @Test
    public void readinessTest() {
        webClient
                .get()
                .uri("/local/readiness")
                .accept(MediaType.TEXT_PLAIN)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .isEqualTo("OK");
    }

    @Test
    public void adminReadinessTest() {
        webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .get()
                .uri("/admin/local/_readiness")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .isEqualTo("OK");
    }

    @Test
    public void livenessTest() {
        webClient
                .get()
                .uri("/local/liveness")
                .accept(MediaType.TEXT_PLAIN)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .isEqualTo("OK");
    }

    @Test
    public void pingTest() {
        webClient
                .get()
                .uri("/ping")
                .accept(MediaType.TEXT_PLAIN)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(String.class)
                .isEqualTo("OK");
    }

}

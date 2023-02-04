package ru.yandex.intranet.d.web.admin.coordination;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.coordination.ClusterStateDto;

/**
 * Coordination admin API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class CoordinationAdminApiTest {

    @Autowired
    private WebTestClient webClient;

    @Test
    public void getClusterStateTest() {
        ClusterStateDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/admin/coordination")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(ClusterStateDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
    }

}

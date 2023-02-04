package ru.yandex.intranet.d.web.front.folders;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestFolders;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.folders.FrontFolderDto;

/**
 * Front folders API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class FrontFoldersApiTest {

    @Autowired
    private WebTestClient webClient;

    @Test
    public void getFolderTest() {
        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/folders/{id}", TestFolders.TEST_FOLDER_1_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontFolderDto.class)
                .isEqualTo(FrontFolderDto.from(TestFolders.TEST_FOLDER_1));
    }

    @Test
    public void getFolderUnauthorizedTest() {
        webClient
                .get()
                .uri("/front/folders/{id}", "a")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    public void getFolderForbiddenTest() {
        webClient
                .mutateWith(MockUser.tvm(TestProviders.YP_SOURCE_TVM_ID))
                .get()
                .uri("/front/folders/{id}", "a")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}

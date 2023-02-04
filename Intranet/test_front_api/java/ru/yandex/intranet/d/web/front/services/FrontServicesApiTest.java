package ru.yandex.intranet.d.web.front.services;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.PageDto;
import ru.yandex.intranet.d.web.model.folders.FrontFolderDto;

/**
 * Front Service API test
 *
 * @author Denis Blokhin <denblo@yandex-team.ru>
 */
@IntegrationTest
public class FrontServicesApiTest {

    @Autowired
    private WebTestClient webClient;

    @Test
    public void getServiceFoldersPageTest() {
        PageDto<FrontFolderDto> result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/services/{id}/folders", 1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<FrontFolderDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getItems().isEmpty());
    }

    @Test
    public void getServiceFoldersNotFoundPageTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/services/{id}/folders", 9999)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getErrors().isEmpty());
    }

    @Test
    public void getServiceFoldersTwoPagesTest() {
        PageDto<FrontFolderDto> firstResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/services/{id}/folders?limit={limit}", 1, 1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<FrontFolderDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(firstResult);
        Assertions.assertEquals(1, firstResult.getItems().size());
        Assertions.assertTrue(firstResult.getNextPageToken().isPresent());
        PageDto<FrontFolderDto> secondResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/services/{id}/folders?limit={limit}&pageToken={token}",
                        1, 1, firstResult.getNextPageToken().get())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<FrontFolderDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(secondResult);
        Assertions.assertFalse(secondResult.getItems().isEmpty());
    }

    @Test
    public void getServiceFoldersPageAcceptableForDAdminsTest() {
        PageDto<FrontFolderDto> result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .get()
                .uri("/front/services/{id}/folders", 1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<FrontFolderDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getItems().isEmpty());
    }
}

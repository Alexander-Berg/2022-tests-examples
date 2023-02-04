package ru.yandex.intranet.d.web.api.folders;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestFolders;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.PageDto;
import ru.yandex.intranet.d.web.model.folders.FolderDto;

/**
 * Folders public API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class FoldersApiTest {

    @Autowired
    private WebTestClient webClient;

    @Test
    public void getFolderTest() {
        FolderDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{id}", "aa6a5d64-5b94-4057-8d43-e65812475e73")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FolderDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals("aa6a5d64-5b94-4057-8d43-e65812475e73", result.getId());
    }

    @Test
    public void getFolderNotFoundTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{id}", "12345678-9012-3456-7890-123456789012")
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
    public void getFoldersPageTest() {
        PageDto<FolderDto> result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<FolderDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getItems().isEmpty());
    }

    @Test
    public void getFoldersTwoPagesTest() {
        PageDto<FolderDto> firstResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders?limit={limit}", 1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<FolderDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(firstResult);
        Assertions.assertEquals(1, firstResult.getItems().size());
        Assertions.assertTrue(firstResult.getNextPageToken().isPresent());
        PageDto<FolderDto> secondResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders?limit={limit}&pageToken={token}",
                        1, firstResult.getNextPageToken().get())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<FolderDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(secondResult);
        Assertions.assertFalse(secondResult.getItems().isEmpty());
    }

    @Test
    public void getServiceFoldersPageTest() {
        PageDto<FolderDto> result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/services/{id}/folders", 1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<FolderDto>>() { })
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
                .uri("/api/v1/services/{id}/folders", 9999)
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
        PageDto<FolderDto> firstResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/services/{id}/folders?limit={limit}", 1, 1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<FolderDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(firstResult);
        Assertions.assertEquals(1, firstResult.getItems().size());
        Assertions.assertEquals("11d1bcdb-3edc-4c21-8a79-4570e3c09c21", firstResult.getItems().get(0).getId());
        Assertions.assertTrue(firstResult.getNextPageToken().isPresent());

        PageDto<FolderDto> secondResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/services/{id}/folders?limit={limit}&pageToken={token}",
                        1, 1, firstResult.getNextPageToken().get())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<FolderDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(secondResult);
        Assertions.assertFalse(secondResult.getItems().isEmpty());
        Assertions.assertEquals("5f73c44f-7569-43d6-980d-7ee663c9789d", secondResult.getItems().get(0).getId());
    }

    @Test
    public void getFolderAcceptableForDAdminsTest() {
        FolderDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .get()
                .uri("/api/v1/folders/{id}", "aa6a5d64-5b94-4057-8d43-e65812475e73")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FolderDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals("aa6a5d64-5b94-4057-8d43-e65812475e73", result.getId());
    }

    @Test
    public void getServiceFoldersPageAcceptableForDAdminsTest() {
        PageDto<FolderDto> result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .get()
                .uri("/api/v1/services/{id}/folders", 1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<FolderDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getItems().isEmpty());
    }

    @Test
    public void getFoldersByIdsTest() {
        PageDto<FolderDto> result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/_by_ids?ids={id1},{id2}", TestFolders.TEST_FOLDER_2_ID,
                        TestFolders.TEST_FOLDER_4_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<FolderDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.getItems().size());
        Set<String> ids = result.getItems().stream().map(FolderDto::getId).collect(Collectors.toSet());
        Assertions.assertEquals(Set.of(TestFolders.TEST_FOLDER_2_ID, TestFolders.TEST_FOLDER_4_ID), ids);
    }

}

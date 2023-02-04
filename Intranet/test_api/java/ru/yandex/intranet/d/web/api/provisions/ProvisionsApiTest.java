package ru.yandex.intranet.d.web.api.provisions;

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
import ru.yandex.intranet.d.web.model.provisions.AccountProvisionDto;

/**
 * Provisions public API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class ProvisionsApiTest {

    @Autowired
    private WebTestClient webClient;

    @Test
    public void getProvisionTest() {
        AccountProvisionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{folderId}/providers/{providerId}/accounts/{accountId}/" +
                                "resources/{resourceId}/provision", "f714c483-c347-41cc-91d0-c6722f5daac7",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", "56a41608-84df-41c4-9653-89106462e0ce",
                        "ef333da9-b076-42f5-b7f5-84cd04ab7fcc")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AccountProvisionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("f714c483-c347-41cc-91d0-c6722f5daac7", result.getFolderId());
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", result.getProviderId());
        Assertions.assertEquals("56a41608-84df-41c4-9653-89106462e0ce", result.getAccountId());
        Assertions.assertEquals("ef333da9-b076-42f5-b7f5-84cd04ab7fcc", result.getResourceId());
        Assertions.assertEquals(200000000L, result.getProvided().longValueExact());
        Assertions.assertEquals("kilobytes", result.getProvidedUnitKey());
        Assertions.assertEquals(100000000L, result.getAllocated().longValueExact());
        Assertions.assertEquals("kilobytes", result.getAllocatedUnitKey());
    }

    @Test
    public void getProvisionNotFoundTest() {
        AccountProvisionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{folderId}/providers/{providerId}/accounts/{accountId}/" +
                                "resources/{resourceId}/provision", "f714c483-c347-41cc-91d0-c6722f5daac7",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", "56a41608-84df-41c4-9653-89106462e0ce",
                        "c79455ac-a88f-40e3-9f6b-117c5c2cd4a2")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AccountProvisionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result);
        Assertions.assertEquals("f714c483-c347-41cc-91d0-c6722f5daac7", result.getFolderId());
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", result.getProviderId());
        Assertions.assertEquals("56a41608-84df-41c4-9653-89106462e0ce", result.getAccountId());
        Assertions.assertEquals("c79455ac-a88f-40e3-9f6b-117c5c2cd4a2", result.getResourceId());
        Assertions.assertEquals(0L, result.getProvided().longValueExact());
        Assertions.assertEquals("gigabytes", result.getProvidedUnitKey());
        Assertions.assertEquals(0L, result.getAllocated().longValueExact());
        Assertions.assertEquals("gigabytes", result.getAllocatedUnitKey());
    }

    @Test
    public void getProvisionFolderNotFoundTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{folderId}/providers/{providerId}/accounts/{accountId}/" +
                                "resources/{resourceId}/provision", "12345678-9012-3456-7890-123456789012",
                        "96e779cf-7d3f-4e74-ba41-c2acc7f04235", "56a41608-84df-41c4-9653-89106462e0ce",
                        "ef333da9-b076-42f5-b7f5-84cd04ab7fcc")
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
    public void getProvisionsPageTest() {
        PageDto<AccountProvisionDto> result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{folderId}/accounts/{accountId}/provisions",
                        "f714c483-c347-41cc-91d0-c6722f5daac7", "9567ae7c-9b76-44bc-87c7-e18d998778b3")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<AccountProvisionDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getItems().isEmpty());
    }

    @Test
    public void getProvisionsTwoPagesTest() {
        PageDto<AccountProvisionDto> firstResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{folderId}/accounts/{accountId}/provisions?limit={limit}",
                        "f714c483-c347-41cc-91d0-c6722f5daac7", "9567ae7c-9b76-44bc-87c7-e18d998778b3", 1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<AccountProvisionDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(firstResult);
        Assertions.assertEquals(1, firstResult.getItems().size());
        Assertions.assertTrue(firstResult.getNextPageToken().isPresent());
        PageDto<AccountProvisionDto> secondResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{folderId}/accounts/{accountId}/provisions?limit={limit}&pageToken={token}",
                        "f714c483-c347-41cc-91d0-c6722f5daac7", "9567ae7c-9b76-44bc-87c7-e18d998778b3",
                        1, firstResult.getNextPageToken().get())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<AccountProvisionDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(secondResult);
        Assertions.assertFalse(secondResult.getItems().isEmpty());
    }

    @Test
    public void getProvisionsFolderNotFoundPageTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{folderId}/accounts/{accountId}/provisions",
                        "12345678-9012-3456-7890-123456789012", "9567ae7c-9b76-44bc-87c7-e18d998778b3")
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
    public void getProvisionsFolderNotFoundTwoPagesTest() {
        PageDto<AccountProvisionDto> firstResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{folderId}/accounts/{accountId}/provisions?limit={limit}",
                        "f714c483-c347-41cc-91d0-c6722f5daac7", "9567ae7c-9b76-44bc-87c7-e18d998778b3", 1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<AccountProvisionDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(firstResult);
        Assertions.assertEquals(1, firstResult.getItems().size());
        Assertions.assertTrue(firstResult.getNextPageToken().isPresent());
        ErrorCollectionDto secondResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{folderId}/accounts/{accountId}/provisions?limit={limit}&pageToken={token}",
                        "12345678-9012-3456-7890-123456789012", "9567ae7c-9b76-44bc-87c7-e18d998778b3",
                        1, firstResult.getNextPageToken().get())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(secondResult);
        Assertions.assertFalse(secondResult.getErrors().isEmpty());
    }

}

package ru.yandex.intranet.d.web.api.quotas;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestFolders;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.TestResources;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.dao.providers.ProvidersDao;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.PageDto;
import ru.yandex.intranet.d.web.model.quotas.FolderQuotaDto;
import ru.yandex.intranet.d.web.model.quotas.FolderResourceQuotaDto;

/**
 * Quotas public API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class QuotasApiTest {

    @Autowired
    private WebTestClient webClient;

    @Autowired
    private ProvidersDao providersDao;

    @Test
    public void getQuotasPageTest() {
        PageDto<FolderQuotaDto> result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{id}/quotas", "aa6a5d64-5b94-4057-8d43-e65812475e73")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<FolderQuotaDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getItems().isEmpty());
    }

    @Test
    public void getQuotasPageNotFoundTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{id}/quotas", "12345678-9012-3456-7890-123456789012")
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
    public void getQuotasPageTwoPagesTest() {
        PageDto<FolderQuotaDto> resultFirst = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{id}/quotas?limit={limit}", "aa6a5d64-5b94-4057-8d43-e65812475e73", 1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<FolderQuotaDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(resultFirst);
        Assertions.assertEquals(1, resultFirst.getItems().size());
        Assertions.assertTrue(resultFirst.getNextPageToken().isPresent());
        PageDto<FolderQuotaDto> resultSecond = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{id}/quotas?pageToken={token}", "aa6a5d64-5b94-4057-8d43-e65812475e73",
                        resultFirst.getNextPageToken().get())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<FolderQuotaDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(resultSecond);
        Assertions.assertFalse(resultSecond.getItems().isEmpty());
    }

    @Test
    public void getResourceQuotaTest() {
        FolderResourceQuotaDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{id}/providers/{providerId}/resources/{resourceId}/quota",
                        "aa6a5d64-5b94-4057-8d43-e65812475e73", "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                        "c79455ac-a88f-40e3-9f6b-117c5c2cd4a2")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FolderResourceQuotaDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
    }

    @Test
    public void getResourceQuotaNotFoundTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{id}/providers/{providerId}/resources/{resourceId}/quota",
                        "12345678-9012-3456-7890-123456789012", "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                        "c79455ac-a88f-40e3-9f6b-117c5c2cd4a2")
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
    public void getQuotasPageAcceptableForDAdminsTest() {
        PageDto<FolderQuotaDto> result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .get()
                .uri("/api/v1/folders/{id}/quotas", "aa6a5d64-5b94-4057-8d43-e65812475e73")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<FolderQuotaDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getItems().isEmpty());
    }

    @Test
    public void getResourceQuotaAcceptableForDAdminsTest() {
        FolderResourceQuotaDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .get()
                .uri("/api/v1/folders/{id}/providers/{providerId}/resources/{resourceId}/quota",
                        "aa6a5d64-5b94-4057-8d43-e65812475e73", "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                        "c79455ac-a88f-40e3-9f6b-117c5c2cd4a2")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FolderResourceQuotaDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
    }

    @Test
    public void getResourceQuotaMissingTest() {
        FolderResourceQuotaDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{id}/providers/{providerId}/resources/{resourceId}/quota",
                        "b2872163-18fb-44ef-9365-b66f7756d636", "96e779cf-7d3f-4e74-ba41-c2acc7f04235",
                        "c79455ac-a88f-40e3-9f6b-117c5c2cd4a2")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FolderResourceQuotaDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0L, result.getQuota().longValueExact());
        Assertions.assertEquals(0L, result.getBalance().longValueExact());
    }

    @Test
    public void getUnmanagedResourceQuotaTest() {
        FolderResourceQuotaDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{id}/providers/{providerId}/resources/{resourceId}/quota",
                        TestFolders.TEST_FOLDER_5_ID, TestProviders.YP_ID, TestResources.YP_HDD_UNMANAGED)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FolderResourceQuotaDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0L, result.getQuota().longValueExact());
        Assertions.assertEquals(0L, result.getBalance().longValueExact());
    }

    @Test
    public void getUnmanagedProviderResourceQuotaTest() {
        FolderResourceQuotaDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{id}/providers/{providerId}/resources/{resourceId}/quota",
                        TestFolders.TEST_FOLDER_WITH_UNMANAGED_PROVIDER_ID,
                        TestProviders.UNMANAGED_PROVIDER_ID,
                        TestResources.UNMANAGED_PROVIDER_RESOURCE_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FolderResourceQuotaDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0L, result.getQuota().longValueExact());
        Assertions.assertEquals(0L, result.getBalance().longValueExact());
    }

    @Test
    public void getProviderResourceQuotasPageTest() {
        PageDto<FolderQuotaDto> result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{id}/quotas",
                        TestFolders.TEST_FOLDER_5_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<FolderQuotaDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getItems().isEmpty());
        FolderQuotaDto quota = result.getItems().stream()
                .filter(q -> q.getResourceId().equals(TestResources.YP_HDD_UNMANAGED))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals(0L, quota.getQuota().longValueExact());
        Assertions.assertEquals(0L, quota.getBalance().longValueExact());
    }

    @Test
    public void getUnmanagedProviderResourceQuotasPageTest() {
        PageDto<FolderQuotaDto> result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/folders/{id}/quotas",
                        TestFolders.TEST_FOLDER_WITH_UNMANAGED_PROVIDER_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<FolderQuotaDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getItems().isEmpty());
        FolderQuotaDto quota = result.getItems().stream()
                .filter(q -> q.getResourceId().equals(TestResources.UNMANAGED_PROVIDER_RESOURCE_ID))
                .findFirst()
                .orElseThrow();
        Assertions.assertEquals(0L, quota.getQuota().longValueExact());
        Assertions.assertEquals(0L, quota.getBalance().longValueExact());
    }
}

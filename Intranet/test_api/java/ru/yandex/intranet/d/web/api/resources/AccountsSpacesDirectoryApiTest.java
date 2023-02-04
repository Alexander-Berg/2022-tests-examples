package ru.yandex.intranet.d.web.api.resources;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestProviders;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.controllers.api.v1.providers.resources.directory.ApiV1AccountsSpacesDirectoryController;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.PageDto;
import ru.yandex.intranet.d.web.model.resources.directory.CreateResourceSegmentationSegmentDto;
import ru.yandex.intranet.d.web.model.resources.directory.spaces.AccountsSpaceCreateDto;
import ru.yandex.intranet.d.web.model.resources.directory.spaces.AccountsSpaceDto;

import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_2;
import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_2_ID;

/**
 * Accounts spaces public API test.
 *
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 * @see ApiV1AccountsSpacesDirectoryController
 * @since 27.01.2021
 */
@IntegrationTest
public class AccountsSpacesDirectoryApiTest {
    private static final String SERVICE_URI = "/api/v1/providers/{providerId}/resourcesDirectory/accountsSpaces";

    @Autowired
    private WebTestClient webClient;

    /**
     * Get accounts space test.
     *
     * @see ApiV1AccountsSpacesDirectoryController#getOne
     */
    @Test
    public void getAccountsSpaceTest() {
        AccountsSpaceDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri(SERVICE_URI + "/{id}",
                        TEST_ACCOUNT_SPACE_2.getProviderId(),
                        TEST_ACCOUNT_SPACE_2.getId()
                )
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AccountsSpaceDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(TEST_ACCOUNT_SPACE_2.getId(), result.getId());
    }

    /**
     * Accounts space not found test.
     *
     * @see ApiV1AccountsSpacesDirectoryController#getOne
     */
    @Test
    public void getAccountsSpaceNotFoundTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri(SERVICE_URI + "/{id}",
                        TEST_ACCOUNT_SPACE_2.getProviderId(),
                        "12345678-9012-3456-7890-123456789012"
                )
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

    /**
     * Get accounts spaces page test.
     *
     * @see ApiV1AccountsSpacesDirectoryController#getPage
     */
    @Test
    public void getAccountsSpacesPageTest() {
        PageDto<AccountsSpaceDto> result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri(SERVICE_URI, TestProviders.YP_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<AccountsSpaceDto>>() {
                })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getItems().isEmpty());
    }

    /**
     * Get two accounts spaces pages test.
     *
     * @see ApiV1AccountsSpacesDirectoryController#getPage
     */
    @Test
    public void getAccountsSpacesTwoPagesTest() {
        PageDto<AccountsSpaceDto> firstResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri(SERVICE_URI + "?limit={limit}",
                        TestProviders.YP_ID, 1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<AccountsSpaceDto>>() {
                })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(firstResult);
        Assertions.assertEquals(1, firstResult.getItems().size());
        Assertions.assertTrue(firstResult.getNextPageToken().isPresent());
        PageDto<AccountsSpaceDto> secondResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri(SERVICE_URI + "?limit={limit}&pageToken={token}",
                        TestProviders.YP_ID, 1, firstResult.getNextPageToken().get())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<AccountsSpaceDto>>() {
                })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(secondResult);
        Assertions.assertFalse(secondResult.getItems().isEmpty());
    }

    /**
     * Create accounts space test.
     *
     * @see ApiV1AccountsSpacesDirectoryController#create
     */
    @Test
    public void createAccountsSpaceTest() {
        AccountsSpaceCreateDto createDto = new AccountsSpaceCreateDto(
                "test", // key
                "Test", // nameEn
                "Тест", // nameRu
                "Description", // descriptionEn
                "Описание", // descriptionRu
                false, // readOnly
                List.of(new CreateResourceSegmentationSegmentDto("7fbd778f-d803-44c8-831a-c1de5c05885c",
                        "8f6a2b58-b10c-4742-bee6-b3587793b5e8")), // segments
                null,
                true);
        AccountsSpaceDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri(SERVICE_URI, "96e779cf-7d3f-4e74-ba41-c2acc7f04235")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(createDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(AccountsSpaceDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getId());
    }

    /**
     * Set read only test.
     *
     * @see ApiV1AccountsSpacesDirectoryController#setReadOnly
     */
    @Test
    public void setReadOnlyTest() {
        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri(SERVICE_URI + "/{id}/_readOnly?readOnly={readOnly}",
                        TestProviders.YP_ID, TEST_ACCOUNT_SPACE_2_ID, true)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk();
    }
}

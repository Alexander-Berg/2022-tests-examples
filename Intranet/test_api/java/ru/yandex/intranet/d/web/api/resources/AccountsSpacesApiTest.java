package ru.yandex.intranet.d.web.api.resources;

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
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.PageDto;
import ru.yandex.intranet.d.web.model.resources.AccountsSpaceDto;

import static ru.yandex.intranet.d.TestAccounts.TEST_ACCOUNT_SPACE_2;

/**
 * Accounts spaces public API test.
 *
 * @see ru.yandex.intranet.d.web.controllers.api.v1.providers.resources.ApiV1AccountsSpacesController
 * @author Vladimir Zaytsev <vzay@yandex-team.ru>
 * @since 27.01.2021
 */
@IntegrationTest
public class AccountsSpacesApiTest {

    @Autowired
    private WebTestClient webClient;

    @Test
    public void getAccountsSpaceTest() {
        AccountsSpaceDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/accountsSpaces/{id}",
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

    @Test
    public void getAccountsSpaceNotFoundTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/accountsSpaces/{id}",
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

    @Test
    public void getAccountsSpacesPageTest() {
        PageDto<AccountsSpaceDto> result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/accountsSpaces",
                        TestProviders.YP_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<AccountsSpaceDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getItems().isEmpty());
    }

    @Test
    public void getAccountsSpacesTwoPagesTest() {
        PageDto<AccountsSpaceDto> firstResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/accountsSpaces?limit={limit}",
                        TestProviders.YP_ID, 1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<AccountsSpaceDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(firstResult);
        Assertions.assertEquals(1, firstResult.getItems().size());
        Assertions.assertTrue(firstResult.getNextPageToken().isPresent());
        PageDto<AccountsSpaceDto> secondResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/providers/{providerId}/accountsSpaces?limit={limit}&pageToken={token}",
                        TestProviders.YP_ID, 1, firstResult.getNextPageToken().get())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<AccountsSpaceDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(secondResult);
        Assertions.assertFalse(secondResult.getItems().isEmpty());
    }
}

package ru.yandex.intranet.d.web.front.providers;

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
import ru.yandex.intranet.d.web.controllers.front.FrontAccountsSpacesController.SelectionTreeNodeDto;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.PageDto;
import ru.yandex.intranet.d.web.model.resources.AccountsSpaceDto;

/**
 * Front accounts spaces API test.
 *
 * @author Denis Blokhin <denblo@yandex-team.ru>
 */
@IntegrationTest
public class FrontAccountsSpacesApiTest {

    @Autowired
    private WebTestClient webClient;

    @Test
    public void getAccountsSpaceNotFoundTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/providers/{providerId}/accountsSpaces",
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
                .uri("/front/providers/{providerId}/accountsSpaces",
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
                .uri("/front/providers/{providerId}/accountsSpaces?limit={limit}",
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
                .uri("/front/providers/{providerId}/accountsSpaces?limit={limit}&pageToken={token}",
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

    @Test
    public void getAccountsSpacesSelectionTreeTest() {
        SelectionTreeNodeDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/providers/{providerId}/accountsSpaces/_selectionTree",
                        TestProviders.ACCOUNTS_SPACES_TREE_PROVIDER_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(SelectionTreeNodeDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals("" +
                "seg1:\n" +
                "A1\n" +
                "   seg2:\n" +
                "   B2\n" +
                "      A1B2\n" +
                "   A2\n" +
                "      A1A2\n" +
                "B1\n" +
                "   seg3:\n" +
                "   B3\n" +
                "      seg4:\n" +
                "      A4\n" +
                "         B1B3A4\n" +
                "   A3\n" +
                "      B1A3\n",
                draw(result, "")
        );
    }

    @Test
    public void getAccountsSpacesSelectionTreeEmptyResultTest() {
        SelectionTreeNodeDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/providers/{providerId}/accountsSpaces/_selectionTree",
                        TestProviders.YDB_ID)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(SelectionTreeNodeDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
    }

    private static String draw(SelectionTreeNodeDto root, String indent) {
        if (root.getValue() != null) {
            return indent + root.getValue().getName() + "\n";
        }
        StringBuilder res = new StringBuilder();
        String segmentationName = root.getSegmentationName();
        res.append(indent).append(segmentationName).append(":\n");
        root.getChildrenBySegmentName().forEach((segmentId, node) ->
                res.append(indent).append(segmentId).append("\n")
                        .append(draw(node, indent + "   "))
        );
        return res.toString();
    }
}

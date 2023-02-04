package ru.yandex.intranet.d.web.api.operations;

import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.operations.OperationDto;
import ru.yandex.intranet.d.web.model.operations.OperationRequestLogDto;
import ru.yandex.intranet.d.web.model.operations.OperationStatusDto;

/**
 * Operations public API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class OperationsApiTest {

    @Autowired
    private WebTestClient webClient;

    @Test
    public void getOperationTest() {
        OperationDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/operations/{id}", "273c0fee-5cf1-4083-b9dc-8ec0e855e150")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(OperationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals("273c0fee-5cf1-4083-b9dc-8ec0e855e150", result.getId());
        Assertions.assertEquals("96e779cf-7d3f-4e74-ba41-c2acc7f04235", result.getProviderId());
        Assertions.assertFalse(result.getAccountsSpaceId().isPresent());
        Assertions.assertEquals(OperationStatusDto.SUCCESS, result.getStatus());
        Assertions.assertEquals(1603385085, result.getCreatedAt().getEpochSecond());
        Assertions.assertFalse(result.getFailure().isPresent());
    }

    @Test
    public void getOperationNotFoundTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/operations/{id}", "12345678-9012-3456-7890-123456789012")
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
    public void getOperationWithErrorTest() {
        OperationDto resultWithError = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/operations/{id}", "4596ab7c-4535-9623-bc12-13ab94ef341d")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(OperationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(resultWithError);
        Assertions.assertEquals("4596ab7c-4535-9623-bc12-13ab94ef341d", resultWithError.getId());
        Assertions.assertEquals(OperationStatusDto.FAILURE, resultWithError.getStatus());
        Assertions.assertTrue(resultWithError.getFailure().isPresent());
        Assertions.assertEquals(Set.of("Test error"), resultWithError.getFailure().get().getErrors());

        OperationDto resultWithFullError = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/operations/{id}", "513cab44-ac21-87da-b99c-bbe10cad123c")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(OperationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(resultWithFullError);
        Assertions.assertEquals("513cab44-ac21-87da-b99c-bbe10cad123c", resultWithFullError.getId());
        Assertions.assertEquals(OperationStatusDto.FAILURE, resultWithFullError.getStatus());
        Assertions.assertTrue(resultWithFullError.getFailure().isPresent());
        Assertions.assertEquals(Set.of("Test error"), resultWithFullError.getFailure().get().getErrors());
    }

    @Test
    public void getOperationRequestLogsTest() {
        OperationDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/operations/{id}", "273c0fee-5cf1-4083-b9dc-8ec0e855e150")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(OperationDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getRequestLogs().isPresent());
        Assertions.assertEquals(1, result.getRequestLogs().get().size());
        OperationRequestLogDto requestLog = result.getRequestLogs().get().get(0);
        Assertions.assertEquals("3223343", requestLog.getRequestId());
    }

}

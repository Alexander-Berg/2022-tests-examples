package ru.yandex.intranet.d.web.api.units;

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
import ru.yandex.intranet.d.web.model.units.UnitDto;
import ru.yandex.intranet.d.web.model.units.UnitsEnsembleDto;

/**
 * Units ensembles public API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class UnitsApiTest {

    @Autowired
    private WebTestClient webClient;

    @Test
    public void getEnsembleTest() {
        UnitsEnsembleDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/unitsEnsembles/{id}", "b02344bf-96af-4cc5-937c-66a479989ce8")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UnitsEnsembleDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals("b02344bf-96af-4cc5-937c-66a479989ce8", result.getId());
    }

    @Test
    public void getEnsembleNotFoundTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/unitsEnsembles/{id}", "12345678-9012-3456-7890-123456789012")
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
    public void getUnitTest() {
        UnitDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/unitsEnsembles/{id}/units/{unitId}", "b02344bf-96af-4cc5-937c-66a479989ce8",
                        "b15101c2-da50-4d6f-9a8e-b90160871b0a")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UnitDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals("b15101c2-da50-4d6f-9a8e-b90160871b0a", result.getId());
    }

    @Test
    public void getUnitNotFoundTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/unitsEnsembles/{id}/units/{unitId}", "b02344bf-96af-4cc5-937c-66a479989ce8",
                        "12345678-9012-3456-7890-123456789012")
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
    public void getEnsemblesPageTest() {
        PageDto<UnitsEnsembleDto> result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/unitsEnsembles")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<UnitsEnsembleDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getItems().isEmpty());
    }

    @Test
    public void getEnsemblesTwoPagesTest() {
        PageDto<UnitsEnsembleDto> firstResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/unitsEnsembles?limit={limit}", 1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<UnitsEnsembleDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(firstResult);
        Assertions.assertEquals(1, firstResult.getItems().size());
        Assertions.assertTrue(firstResult.getNextPageToken().isPresent());
        PageDto<UnitsEnsembleDto> secondResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/api/v1/unitsEnsembles?limit={limit}&pageToken={token}",
                        1, firstResult.getNextPageToken().get())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<UnitsEnsembleDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(secondResult);
        Assertions.assertFalse(secondResult.getItems().isEmpty());
    }

    @Test
    public void getEnsembleAcceptableForDAdminsTest() {
        UnitsEnsembleDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .get()
                .uri("/api/v1/unitsEnsembles/{id}", "b02344bf-96af-4cc5-937c-66a479989ce8")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(UnitsEnsembleDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals("b02344bf-96af-4cc5-937c-66a479989ce8", result.getId());
    }

}

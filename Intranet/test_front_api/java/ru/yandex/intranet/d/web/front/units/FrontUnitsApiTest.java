package ru.yandex.intranet.d.web.front.units;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.i18n.Locales;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.PageDto;
import ru.yandex.intranet.d.web.model.units.front.FrontUnitDto;
import ru.yandex.intranet.d.web.model.units.front.FrontUnitsEnsembleDto;
import ru.yandex.intranet.d.web.model.units.front.PluralFormDto;

/**
 * Front units ensembles public API test.
 *
 * @author Denis Blokhin <denblo@yandex-team.ru>
 */
@IntegrationTest
public class FrontUnitsApiTest {

    @Autowired
    private WebTestClient webClient;

    @Test
    public void getEnsembleTest() {
        FrontUnitsEnsembleDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/unitsEnsembles/{id}", "b02344bf-96af-4cc5-937c-66a479989ce8")
                .header("Accept-Language", Locales.RUSSIAN.toLanguageTag())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontUnitsEnsembleDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals("b02344bf-96af-4cc5-937c-66a479989ce8", result.getId());
        FrontUnitDto unit = result.getUnits().stream()
                .filter(e -> e.getKey().equals("exabytes"))
                .findFirst().get();

        Assertions.assertEquals(Map.of(
                PluralFormDto.ONE, "exabyte",
                PluralFormDto.SOME, "exabyte",
                PluralFormDto.MANY, "exabytes",
                PluralFormDto.NONE, "exabytes"
        ), unit.getLongName());

        result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/unitsEnsembles/{id}", "b02344bf-96af-4cc5-937c-66a479989ce8")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FrontUnitsEnsembleDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals("b02344bf-96af-4cc5-937c-66a479989ce8", result.getId());
        unit = result.getUnits().stream()
                .filter(e -> e.getKey().equals("exabytes"))
                .findFirst().get();

        Assertions.assertEquals(Map.of(
                PluralFormDto.ONE, "exabyte",
                PluralFormDto.SOME, "exabytes",
                PluralFormDto.MANY, "exabytes",
                PluralFormDto.NONE, "exabytes"
        ), unit.getLongName());
    }

    @Test
    public void getEnsembleNotFoundTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/unitsEnsembles/{id}", "12345678-9012-3456-7890-123456789012")
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
        PageDto<FrontUnitsEnsembleDto> result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/unitsEnsembles")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<FrontUnitsEnsembleDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getItems().isEmpty());
    }

    @Test
    public void getEnsemblesTwoPagesTest() {
        PageDto<FrontUnitsEnsembleDto> firstResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/unitsEnsembles?limit={limit}", 1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<FrontUnitsEnsembleDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(firstResult);
        Assertions.assertEquals(1, firstResult.getItems().size());
        Assertions.assertTrue(firstResult.getNextPageToken().isPresent());
        PageDto<FrontUnitsEnsembleDto> secondResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/front/unitsEnsembles?limit={limit}&pageToken={token}",
                        1, firstResult.getNextPageToken().get())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<FrontUnitsEnsembleDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(secondResult);
        Assertions.assertFalse(secondResult.getItems().isEmpty());
    }
}

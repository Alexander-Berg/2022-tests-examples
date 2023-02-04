package ru.yandex.intranet.d.web.admin.units;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.yandex.ydb.table.transaction.TransactionMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.dao.Tenants;
import ru.yandex.intranet.d.dao.units.UnitsEnsemblesDao;
import ru.yandex.intranet.d.datasource.model.YdbTableClient;
import ru.yandex.intranet.d.model.units.GrammaticalCase;
import ru.yandex.intranet.d.model.units.UnitModel;
import ru.yandex.intranet.d.model.units.UnitsEnsembleModel;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.PageDto;
import ru.yandex.intranet.d.web.model.units.FullUnitDto;
import ru.yandex.intranet.d.web.model.units.FullUnitsEnsembleDto;
import ru.yandex.intranet.d.web.model.units.GrammaticalCaseDto;
import ru.yandex.intranet.d.web.model.units.UnitCreateDto;
import ru.yandex.intranet.d.web.model.units.UnitPutDto;
import ru.yandex.intranet.d.web.model.units.UnitsEnsembleCreateDto;
import ru.yandex.intranet.d.web.model.units.UnitsEnsemblePutDto;

/**
 * Units ensembles admin API test.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@IntegrationTest
public class AdminUnitsApiTest {

    @Autowired
    private WebTestClient webClient;

    @Autowired
    private UnitsEnsemblesDao unitsEnsemblesDao;

    @Autowired
    private YdbTableClient tableClient;

    @Test
    public void getEnsembleTest() {
        FullUnitsEnsembleDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/admin/unitsEnsembles/{id}", "b02344bf-96af-4cc5-937c-66a479989ce8")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullUnitsEnsembleDto.class)
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
                .uri("/admin/unitsEnsembles/{id}", "12345678-9012-3456-7890-123456789012")
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
        FullUnitDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/admin/unitsEnsembles/{id}/units/{unitId}", "b02344bf-96af-4cc5-937c-66a479989ce8",
                        "b15101c2-da50-4d6f-9a8e-b90160871b0a")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullUnitDto.class)
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
                .uri("/admin/unitsEnsembles/{id}/units/{unitId}", "b02344bf-96af-4cc5-937c-66a479989ce8",
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
        PageDto<FullUnitsEnsembleDto> result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/admin/unitsEnsembles")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<FullUnitsEnsembleDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getItems().isEmpty());
    }

    @Test
    public void getEnsemblesTwoPagesTest() {
        PageDto<FullUnitsEnsembleDto> firstResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/admin/unitsEnsembles?limit={limit}", 1)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<FullUnitsEnsembleDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(firstResult);
        Assertions.assertEquals(1, firstResult.getItems().size());
        Assertions.assertTrue(firstResult.getNextPageToken().isPresent());
        PageDto<FullUnitsEnsembleDto> secondResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/admin/unitsEnsembles?limit={limit}&pageToken={token}",
                        1, firstResult.getNextPageToken().get())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(new ParameterizedTypeReference<PageDto<FullUnitsEnsembleDto>>() { })
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(secondResult);
        Assertions.assertFalse(secondResult.getItems().isEmpty());
    }

    @Test
    public void createEnsembleTest() {
        UnitCreateDto unitOne = new UnitCreateDto("grams",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                0L);
        UnitCreateDto unitTwo = new UnitCreateDto("kilograms",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                3L);
        UnitsEnsembleCreateDto body = new UnitsEnsembleCreateDto("Test", "Тест", "Test description",
                "Тестовое описание", true, List.of(unitOne, unitTwo), "test");
        FullUnitsEnsembleDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/admin/unitsEnsembles")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullUnitsEnsembleDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getId());
    }

    @Test
    public void createEnsembleErrorTest() {
        UnitCreateDto unitOne = new UnitCreateDto("grams",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                0L);
        UnitCreateDto unitTwo = new UnitCreateDto("kilograms",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                3L);
        UnitsEnsembleCreateDto body = new UnitsEnsembleCreateDto("", "", "Test description",
                "Тестовое описание", true, List.of(unitOne, unitTwo), "test");
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/admin/unitsEnsembles")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
    }

    @Test
    public void createEnsembleKeyConflictTest() {
        UnitCreateDto unitOne = new UnitCreateDto("grams",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                0L);
        UnitCreateDto unitTwo = new UnitCreateDto("kilograms",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                3L);
        UnitsEnsembleCreateDto body = new UnitsEnsembleCreateDto("Test", "Тест", "Test description",
                "Тестовое описание", true, List.of(unitOne, unitTwo), "storageUnitsDecimal");
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/admin/unitsEnsembles")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.CONFLICT)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getFieldErrors().isEmpty());
    }

    @Test
    public void createUnitTest() {
        UnitCreateDto unitOne = new UnitCreateDto("grams",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                0L);
        UnitCreateDto unitTwo = new UnitCreateDto("kilograms",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                3L);
        UnitsEnsembleCreateDto body = new UnitsEnsembleCreateDto("Test", "Тест", "Test description",
                "Тестовое описание", true, List.of(unitOne, unitTwo), "test");
        FullUnitsEnsembleDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/admin/unitsEnsembles")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullUnitsEnsembleDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getId());
        UnitCreateDto unitThree = new UnitCreateDto("tons",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                6L);
        FullUnitsEnsembleDto unitResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/admin/unitsEnsembles/{id}/units?version={version}", result.getId(), result.getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(unitThree)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullUnitsEnsembleDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(unitResult);
        Assertions.assertEquals(3, unitResult.getUnits().size());
    }

    @Test
    public void createUnitErrorTest() {
        UnitCreateDto unitOne = new UnitCreateDto("grams",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                0L);
        UnitCreateDto unitTwo = new UnitCreateDto("kilograms",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                3L);
        UnitsEnsembleCreateDto body = new UnitsEnsembleCreateDto("Test", "Тест", "Test description",
                "Тестовое описание", true, List.of(unitOne, unitTwo), "test");
        FullUnitsEnsembleDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/admin/unitsEnsembles")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullUnitsEnsembleDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getId());
        UnitCreateDto unitThree = new UnitCreateDto("",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                6L);
        ErrorCollectionDto unitResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/admin/unitsEnsembles/{id}/units?version={version}", result.getId(), result.getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(unitThree)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(unitResult);
        Assertions.assertFalse(unitResult.getFieldErrors().isEmpty());
    }

    @Test
    public void putEnsembleTest() {
        UnitCreateDto unitOne = new UnitCreateDto("grams",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                0L);
        UnitCreateDto unitTwo = new UnitCreateDto("kilograms",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                3L);
        UnitsEnsembleCreateDto body = new UnitsEnsembleCreateDto("Test", "Тест", "Test description",
                "Тестовое описание", true, List.of(unitOne, unitTwo), "test");
        FullUnitsEnsembleDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/admin/unitsEnsembles")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullUnitsEnsembleDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getId());
        UnitsEnsemblePutDto putBody = new UnitsEnsemblePutDto("Test update", "Тест изменения",
                "Test description update", "Тестовое описание изменение");
        FullUnitsEnsembleDto putResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/admin/unitsEnsembles/{id}?version={version}", result.getId(), result.getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(putBody)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullUnitsEnsembleDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(putResult);
        Assertions.assertEquals(putBody.getNameEn().orElse(null), putResult.getNameEn());
        Assertions.assertEquals(putBody.getNameRu().orElse(null), putResult.getNameRu());
        Assertions.assertEquals(putBody.getDescriptionEn().orElse(null), putResult.getDescriptionEn());
        Assertions.assertEquals(putBody.getDescriptionRu().orElse(null), putResult.getDescriptionRu());
    }

    @Test
    public void putEnsembleErrorTest() {
        UnitCreateDto unitOne = new UnitCreateDto("grams",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                0L);
        UnitCreateDto unitTwo = new UnitCreateDto("kilograms",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                3L);
        UnitsEnsembleCreateDto body = new UnitsEnsembleCreateDto("Test", "Тест", "Test description",
                "Тестовое описание", true, List.of(unitOne, unitTwo), "test");
        FullUnitsEnsembleDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/admin/unitsEnsembles")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullUnitsEnsembleDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getId());
        UnitsEnsemblePutDto putBody = new UnitsEnsemblePutDto("", "Тест изменения",
                "Test description update", "Тестовое описание изменение");
        ErrorCollectionDto putResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/admin/unitsEnsembles/{id}?version={version}", result.getId(), result.getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(putBody)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(putResult);
        Assertions.assertFalse(putResult.getFieldErrors().isEmpty());
    }

    @Test
    public void putUnitTest() {
        UnitCreateDto unitOne = new UnitCreateDto("grams",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                0L);
        UnitCreateDto unitTwo = new UnitCreateDto("kilograms",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                3L);
        UnitsEnsembleCreateDto body = new UnitsEnsembleCreateDto("Test", "Тест", "Test description",
                "Тестовое описание", true, List.of(unitOne, unitTwo), "test");
        FullUnitsEnsembleDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/admin/unitsEnsembles")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullUnitsEnsembleDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getId());
        Optional<FullUnitDto> unitToUpdate = result.getUnits().stream().filter(u -> u.getPower() == 3).findFirst();
        Assertions.assertTrue(unitToUpdate.isPresent());
        UnitPutDto putUnitDto = new UnitPutDto(
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE up",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE up",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE up",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE up",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL up",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL up"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE up",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE up",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE up",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE up",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL up",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL up"),
                "short name singular up",
                "short name plural up",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE up",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE up",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE up",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE up",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL up",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL up"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE up",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE up",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE up",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE up",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL up",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL up"),
                "long name singular up",
                "long name plural up",
                10L,
                3L);
        FullUnitsEnsembleDto putResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/admin/unitsEnsembles/{id}/units/{unitId}?version={version}",
                        result.getId(), unitToUpdate.get().getId(), result.getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(putUnitDto)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullUnitsEnsembleDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(putResult);
        Assertions.assertEquals(result.getVersion() + 1, putResult.getVersion());
    }

    @Test
    public void putUnitErrorTest() {
        UnitCreateDto unitOne = new UnitCreateDto("grams",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                0L);
        UnitCreateDto unitTwo = new UnitCreateDto("kilograms",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                3L);
        UnitsEnsembleCreateDto body = new UnitsEnsembleCreateDto("Test", "Тест", "Test description",
                "Тестовое описание", true, List.of(unitOne, unitTwo), "test");
        FullUnitsEnsembleDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/admin/unitsEnsembles")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullUnitsEnsembleDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getId());
        Optional<FullUnitDto> unitToUpdate = result.getUnits().stream().filter(u -> u.getPower() == 3).findFirst();
        Assertions.assertTrue(unitToUpdate.isPresent());
        UnitPutDto putUnitDto = new UnitPutDto(
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE up",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE up",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE up",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE up",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL up",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL up"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE up",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE up",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE up",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE up",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL up",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL up"),
                "",
                "",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE up",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE up",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE up",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE up",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL up",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL up"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE up",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE up",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE up",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE up",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL up",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL up"),
                "long name singular up",
                "long name plural up",
                10L,
                3L);
        ErrorCollectionDto putResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .put()
                .uri("/admin/unitsEnsembles/{id}/units/{unitId}?version={version}",
                        result.getId(), unitToUpdate.get().getId(), result.getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(putUnitDto)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(putResult);
        Assertions.assertFalse(putResult.getFieldErrors().isEmpty());
    }

    @Test
    public void patchEnsembleTest() {
        UnitCreateDto unitOne = new UnitCreateDto("grams",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                0L);
        UnitCreateDto unitTwo = new UnitCreateDto("kilograms",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                3L);
        UnitsEnsembleCreateDto body = new UnitsEnsembleCreateDto("Test", "Тест", "Test description",
                "Тестовое описание", true, List.of(unitOne, unitTwo), "test");
        FullUnitsEnsembleDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/admin/unitsEnsembles")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullUnitsEnsembleDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getId());
        Map<String, Object> patchBody = new HashMap<>();
        patchBody.put("nameEn", "Test patch");
        patchBody.put("nameRu", "Test patch ru");
        FullUnitsEnsembleDto patchResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .patch()
                .uri("/admin/unitsEnsembles/{id}?version={version}", result.getId(), result.getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(patchBody)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullUnitsEnsembleDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(patchResult);
        Assertions.assertEquals(patchBody.get("nameEn"), patchResult.getNameEn());
        Assertions.assertEquals(patchBody.get("nameRu"), patchResult.getNameRu());
        Assertions.assertEquals(body.getDescriptionEn().orElseThrow(), patchResult.getDescriptionEn());
        Assertions.assertEquals(body.getDescriptionRu().orElseThrow(), patchResult.getDescriptionRu());
    }

    @Test
    public void deleteEnsembleTest() {
        UnitCreateDto unitOne = new UnitCreateDto("grams",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                0L);
        UnitCreateDto unitTwo = new UnitCreateDto("kilograms",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                3L);
        UnitsEnsembleCreateDto body = new UnitsEnsembleCreateDto("Test", "Тест", "Test description",
                "Тестовое описание", true, List.of(unitOne, unitTwo), "test");
        FullUnitsEnsembleDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/admin/unitsEnsembles")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullUnitsEnsembleDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getId());
        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .delete()
                .uri("/admin/unitsEnsembles/{id}?version={version}",
                        result.getId(), result.getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNoContent();
    }

    @Test
    public void deleteEnsembleErrorTest() {
        UnitCreateDto unitOne = new UnitCreateDto("grams",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                0L);
        UnitCreateDto unitTwo = new UnitCreateDto("kilograms",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                3L);
        UnitsEnsembleCreateDto body = new UnitsEnsembleCreateDto("Test", "Тест", "Test description",
                "Тестовое описание", true, List.of(unitOne, unitTwo), "test");
        FullUnitsEnsembleDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/admin/unitsEnsembles")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullUnitsEnsembleDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getId());
        ErrorCollectionDto putResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .delete()
                .uri("/admin/unitsEnsembles/{id}?version={version}",
                        result.getId(), result.getVersion() + 2L)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.PRECONDITION_FAILED)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(putResult);
        Assertions.assertFalse(putResult.getFieldErrors().isEmpty());
    }

    @Test
    public void deleteUnitTest() {
        UnitCreateDto unitOne = new UnitCreateDto("grams",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                0L);
        UnitCreateDto unitTwo = new UnitCreateDto("kilograms",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                3L);
        UnitsEnsembleCreateDto body = new UnitsEnsembleCreateDto("Test", "Тест", "Test description",
                "Тестовое описание", true, List.of(unitOne, unitTwo), "test");
        FullUnitsEnsembleDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/admin/unitsEnsembles")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullUnitsEnsembleDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getId());
        Optional<FullUnitDto> unitToDelete = result.getUnits().stream().filter(u -> u.getPower() == 3).findFirst();
        Assertions.assertTrue(unitToDelete.isPresent());
        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .delete()
                .uri("/admin/unitsEnsembles/{id}/units/{unitId}?version={version}",
                        result.getId(), unitToDelete.get().getId(), result.getVersion())
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNoContent();
    }

    @Test
    public void deleteUnitErrorTest() {
        UnitCreateDto unitOne = new UnitCreateDto("grams",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                0L);
        UnitCreateDto unitTwo = new UnitCreateDto("kilograms",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                3L);
        UnitsEnsembleCreateDto body = new UnitsEnsembleCreateDto("Test", "Тест", "Test description",
                "Тестовое описание", true, List.of(unitOne, unitTwo), "test");
        FullUnitsEnsembleDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/admin/unitsEnsembles")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullUnitsEnsembleDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getId());
        Optional<FullUnitDto> unitToDelete = result.getUnits().stream().filter(u -> u.getPower() == 3).findFirst();
        Assertions.assertTrue(unitToDelete.isPresent());
        ErrorCollectionDto putResult = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .delete()
                .uri("/admin/unitsEnsembles/{id}/units/{unitId}?version={version}",
                        result.getId(), unitToDelete.get().getId(), result.getVersion() + 2)
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isEqualTo(HttpStatus.PRECONDITION_FAILED)
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(putResult);
        Assertions.assertFalse(putResult.getFieldErrors().isEmpty());
    }

    @Test
    public void createEnsembleAcceptableForDAdminsTest() {
        UnitCreateDto unitOne = new UnitCreateDto("grams",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                0L);
        UnitCreateDto unitTwo = new UnitCreateDto("kilograms",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                3L);
        UnitsEnsembleCreateDto body = new UnitsEnsembleCreateDto("Test", "Тест", "Test description",
                "Тестовое описание", true, List.of(unitOne, unitTwo), "test");
        FullUnitsEnsembleDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.D_ADMIN_UID))
                .post()
                .uri("/admin/unitsEnsembles")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullUnitsEnsembleDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getId());
    }

    @Test
    public void createEnsembleForbiddenForNonDAdminsTest() {
        UnitCreateDto unitOne = new UnitCreateDto("grams",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                0L);
        UnitCreateDto unitTwo = new UnitCreateDto("kilograms",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                3L);
        UnitsEnsembleCreateDto body = new UnitsEnsembleCreateDto("Test", "Тест", "Test description",
                "Тестовое описание", true, List.of(unitOne, unitTwo), "test");
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.NOT_D_ADMIN_UID))
                .post()
                .uri("/admin/unitsEnsembles")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isForbidden()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.getErrors().isEmpty());
    }

    @Test
    public void createAndCheckDatabaseTest() {
        UnitCreateDto unit = new UnitCreateDto("grams",
                Map.of(
                        GrammaticalCaseDto.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(
                        GrammaticalCaseDto.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "short plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "short plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(
                        GrammaticalCaseDto.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long singular GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long singular DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(
                        GrammaticalCaseDto.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCaseDto.GENITIVE, "long plural GENITIVE",
                        GrammaticalCaseDto.DATIVE, "long plural DATIVE",
                        GrammaticalCaseDto.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCaseDto.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCaseDto.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                0L);
        UnitsEnsembleCreateDto body = new UnitsEnsembleCreateDto("Test", "Тест", "Test description",
                "Тестовое описание", true, List.of(unit), "test");
        FullUnitsEnsembleDto result = webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .post()
                .uri("/admin/unitsEnsembles")
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(FullUnitsEnsembleDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getId());

        Optional<UnitsEnsembleModel> unitsEnsembleModel = tableClient.usingSessionMonoRetryable(session ->
                unitsEnsemblesDao.getById(session.asTxCommitRetryable(TransactionMode.STALE_READ_ONLY),
                        result.getId(), Tenants.DEFAULT_TENANT_ID)
        ).block();
        Assertions.assertNotNull(unitsEnsembleModel);
        Assertions.assertTrue(unitsEnsembleModel.isPresent());
        Set<UnitModel> units = unitsEnsembleModel.get().getUnits();

        Set<UnitModel> exceptedUnits = Set.of(new UnitModel(
                result.getUnits().get(0).getId(),
                "grams",
                Map.of(
                        GrammaticalCase.NOMINATIVE, "short singular NOMINATIVE",
                        GrammaticalCase.GENITIVE, "short singular GENITIVE",
                        GrammaticalCase.DATIVE, "short singular DATIVE",
                        GrammaticalCase.ACCUSATIVE, "short singular ACCUSATIVE",
                        GrammaticalCase.INSTRUMENTAL, "short singular INSTRUMENTAL",
                        GrammaticalCase.PREPOSITIONAL, "short singular PREPOSITIONAL"),
                Map.of(
                        GrammaticalCase.NOMINATIVE, "short plural NOMINATIVE",
                        GrammaticalCase.GENITIVE, "short plural GENITIVE",
                        GrammaticalCase.DATIVE, "short plural DATIVE",
                        GrammaticalCase.ACCUSATIVE, "short plural ACCUSATIVE",
                        GrammaticalCase.INSTRUMENTAL, "short plural INSTRUMENTAL",
                        GrammaticalCase.PREPOSITIONAL, "short plural PREPOSITIONAL"),
                "short name singular",
                "short name plural",
                Map.of(
                        GrammaticalCase.NOMINATIVE, "long singular NOMINATIVE",
                        GrammaticalCase.GENITIVE, "long singular GENITIVE",
                        GrammaticalCase.DATIVE, "long singular DATIVE",
                        GrammaticalCase.ACCUSATIVE, "long singular ACCUSATIVE",
                        GrammaticalCase.INSTRUMENTAL, "long singular INSTRUMENTAL",
                        GrammaticalCase.PREPOSITIONAL, "long singular PREPOSITIONAL"),
                Map.of(
                        GrammaticalCase.NOMINATIVE, "long plural NOMINATIVE",
                        GrammaticalCase.GENITIVE, "long plural GENITIVE",
                        GrammaticalCase.DATIVE, "long plural DATIVE",
                        GrammaticalCase.ACCUSATIVE, "long plural ACCUSATIVE",
                        GrammaticalCase.INSTRUMENTAL, "long plural INSTRUMENTAL",
                        GrammaticalCase.PREPOSITIONAL, "long plural PREPOSITIONAL"),
                "long name singular",
                "long name plural",
                10L,
                0L,
                false
        ));
        Assertions.assertEquals(exceptedUnits, units);
    }

}

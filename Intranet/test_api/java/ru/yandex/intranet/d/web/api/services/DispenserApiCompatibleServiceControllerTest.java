package ru.yandex.intranet.d.web.api.services;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import ru.yandex.intranet.d.IntegrationTest;
import ru.yandex.intranet.d.TestUsers;
import ru.yandex.intranet.d.web.MockUser;
import ru.yandex.intranet.d.web.model.ErrorCollectionDto;
import ru.yandex.intranet.d.web.model.legacy.DispenserGetProjectsListResponseDto;
import ru.yandex.intranet.d.web.model.legacy.DispenserGetProjectsResponseDto;

/**
 * Tests for service (project) dispenser controller: /dispenser/common/api/v1/projects
 *
 * @author Evgenii Serov <evserov@yandex-team.ru>
 */
@IntegrationTest
public class DispenserApiCompatibleServiceControllerTest {

    @Autowired
    private WebTestClient webClient;
    @Value("${hardwareOrderService.tvmSourceId}")
    private long hardwareOrderServiceTvmSourceId;

    @Test
    public void dispenserGetProjectsTest() {
        DispenserGetProjectsListResponseDto result = webClient
                .mutateWith(MockUser.tvm(hardwareOrderServiceTvmSourceId))
                .get()
                .uri("/dispenser/common/api/v1/projects?project=market,mbo,dispenser")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(DispenserGetProjectsListResponseDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.getResult().size());

        Optional<DispenserGetProjectsResponseDto> mbo = result.getResult().stream()
                .filter(res -> res.getKey().equals("mbo")).findFirst();
        Assertions.assertTrue(mbo.isPresent());
        Assertions.assertEquals(1, mbo.get().getSubprojectKeys().size());
        Assertions.assertTrue(mbo.get().getSubprojectKeys().contains("cms"));

        Optional<DispenserGetProjectsResponseDto> market = result.getResult().stream()
                .filter(res -> res.getKey().equals("market")).findFirst();
        Assertions.assertTrue(market.isPresent());
        Assertions.assertEquals(2, market.get().getSubprojectKeys().size());
        Assertions.assertTrue(market.get().getSubprojectKeys().contains("ir"));
        Assertions.assertTrue(market.get().getSubprojectKeys().contains("mbo"));

        Optional<DispenserGetProjectsResponseDto> dispenser = result.getResult().stream()
                .filter(res -> res.getKey().equals("dispenser")).findFirst();
        Assertions.assertTrue(dispenser.isPresent());
        Assertions.assertEquals(0, dispenser.get().getSubprojectKeys().size());
    }

    @Test
    public void dispenserGetProjectTest() {
        DispenserGetProjectsListResponseDto result = webClient
                .mutateWith(MockUser.tvm(hardwareOrderServiceTvmSourceId))
                .get()
                .uri("/dispenser/common/api/v1/projects?project=dispenser")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isOk()
                .expectBody(DispenserGetProjectsListResponseDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getResult().size());

        Optional<DispenserGetProjectsResponseDto> dispenser = result.getResult().stream()
                .filter(res -> res.getKey().equals("dispenser")).findFirst();
        Assertions.assertTrue(dispenser.isPresent());
        Assertions.assertEquals(0, dispenser.get().getSubprojectKeys().size());
    }

    @Test
    public void dispenserGetEmptyProjectsTest() {
        String result = webClient
                .mutateWith(MockUser.tvm(hardwareOrderServiceTvmSourceId))
                .get()
                .uri("/dispenser/common/api/v1/projects?project=")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isBadRequest()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals("Parameter 'project' is required.", result);
    }

    @Test
    public void dispenserGetProjectNotFoundTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.tvm(hardwareOrderServiceTvmSourceId))
                .get()
                .uri("/dispenser/common/api/v1/projects?project=notFound")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getErrors().size());
        Assertions.assertTrue(result.getErrors().contains("Service not found."));
    }

    @Test
    public void dispenserGetProjectsNotFoundTest() {
        ErrorCollectionDto result = webClient
                .mutateWith(MockUser.tvm(hardwareOrderServiceTvmSourceId))
                .get()
                .uri("/dispenser/common/api/v1/projects?project=market,mbo,notFound")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isNotFound()
                .expectBody(ErrorCollectionDto.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getErrors().size());
        Assertions.assertTrue(result.getErrors().contains("Service not found."));
    }

    @Test
    public void dispenserGetProjectPermissionDeniedTest() {
        webClient
                .mutateWith(MockUser.uid(TestUsers.USER_1_UID))
                .get()
                .uri("/dispenser/common/api/v1/projects?project=notFound")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus()
                .isForbidden();
    }
}
